import { useEffect } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap, useMapEvents } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { renderToStaticMarkup } from 'react-dom/server';
import { Warehouse, Package, MapPin, User } from 'lucide-react';
import type { MapData, DeliveryRequest, CustomStop } from '../types';

// Helper to create DivIcon from Lucide component
const createLucideIcon = (icon: React.ReactNode, color: string) => {
  const iconHtml = renderToStaticMarkup(
    <div style={{ 
      color: color, 
      filter: 'drop-shadow(3px 3px 0px rgba(0,0,0,1))',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }}>
      {icon}
    </div>
  );

  return L.divIcon({
    html: iconHtml,
    className: 'custom-lucide-icon',
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32]
  });
};

const warehouseIcon = createLucideIcon(<Warehouse size={32} fill="currentColor" />, '#fca5a5'); // Pastel Red
const pickupIcon = createLucideIcon(<Package size={32} fill="currentColor" />, '#93c5fd'); // Pastel Blue
const deliveryIcon = createLucideIcon(<MapPin size={32} fill="currentColor" />, '#86efac'); // Pastel Green
const userIcon = createLucideIcon(<User size={32} fill="currentColor" />, '#fde047'); // Pastel Yellow

interface MapComponentProps {
  mapData: MapData | null;
  deliveryRequest: DeliveryRequest | null;
  userLocation: { lat: number; lng: number } | null;
  customStops: CustomStop[];
  onMapClick: (lat: number, lng: number) => void;
  tspPaths: string[][]; // Multiple paths, one per courier
}

function MapController({ bounds }: { bounds: [number, number][] }) {
  const map = useMap();
  useEffect(() => {
    if (bounds) {
      map.fitBounds(bounds);
    }
  }, [map, bounds]);
  return null;
}

function MapClickHandler({ onClick }: { onClick: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onClick(e.latlng.lat, e.latlng.lng);
    },
  });
  return null;
}

const MapComponent: React.FC<MapComponentProps> = ({ 
  mapData, 
  deliveryRequest, 
  userLocation, 
  onMapClick,
  customStops,
  tspPaths
}) => {
  const center: [number, number] = [45.75, 4.85]; // Default Lyon

  // Define different colors for each courier
  const courierColors = ['#ef4444', '#3b82f6', '#10b981', '#f59e0b', '#8b5cf6'];

  return (
    <MapContainer 
      center={center} 
      zoom={13} 
      style={{ height: '100%', width: '100%' }}
      attributionControl={false}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
      />
      
      {mapData && (
        <>
          <MapController bounds={mapData.bounds} />
          {mapData.segments.map((segment, idx) => {
            const origin = mapData.nodes.get(segment.origin);
            const dest = mapData.nodes.get(segment.destination);
            if (origin && dest) {
              return (
                <Polyline
                  key={idx}
                  positions={[
                    [origin.latitude, origin.longitude],
                    [dest.latitude, dest.longitude]
                  ]}
                  color="#d1d5db" // light gray
                  weight={4}
                  opacity={0.8}
                />
              );
            }
            return null;
          })}
        </>
      )}

      {deliveryRequest && mapData && (
        <>
          {/* Warehouse */}
          {(() => {
            const node = mapData.nodes.get(deliveryRequest.warehouse.nodeId);
            if (node) {
              return (
                <Marker position={[node.latitude, node.longitude]} icon={warehouseIcon}>
                  <Popup>Warehouse (Depot)</Popup>
                </Marker>
              );
            }
            return null;
          })()}

          {/* Deliveries */}
          {deliveryRequest.deliveries.map((delivery) => {
            const pickupNode = mapData.nodes.get(delivery.pickupNodeId);
            const deliveryNode = mapData.nodes.get(delivery.deliveryNodeId);
            
            return (
              <div key={delivery.id}>
                {pickupNode && (
                  <Marker position={[pickupNode.latitude, pickupNode.longitude]} icon={pickupIcon}>
                    <Popup>Pickup: {delivery.id}</Popup>
                  </Marker>
                )}
                {deliveryNode && (
                  <Marker position={[deliveryNode.latitude, deliveryNode.longitude]} icon={deliveryIcon}>
                    <Popup>Delivery: {delivery.id}</Popup>
                  </Marker>
                )}
              </div>
            );
          })}
        </>
      )}

      {customStops.map((stop, idx) => (
        <Marker 
          key={`custom-${idx}`} 
          position={[stop.latitude, stop.longitude]} 
          icon={stop.type === 'warehouse' ? warehouseIcon : stop.type === 'pickup' ? pickupIcon : deliveryIcon}
        >
          <Popup>Custom {stop.type}</Popup>
        </Marker>
      ))}

      {userLocation && (
        <Marker position={[userLocation.lat, userLocation.lng]} icon={userIcon}>
          <Popup>Your Location</Popup>
        </Marker>
      )}

      {/* TSP Paths with arrows - one per courier */}
      {tspPaths.length > 0 && mapData && tspPaths.map((tspPath, courierIdx) => {
        const courierColor = courierColors[courierIdx % courierColors.length];

        return (
          <div key={`courier-${courierIdx}`}>
            <Polyline
              positions={tspPath
                .map(nodeId => mapData.nodes.get(nodeId))
                .filter(node => node !== undefined)
                .map(node => [node!.latitude, node!.longitude] as [number, number])
              }
              color={courierColor}
              weight={5}
              opacity={0.8}
              pathOptions={{
                className: `tsp-path-courier-${courierIdx}`
              }}
            >
              <Popup>
                <div>
                  <strong>Courier {courierIdx + 1} Route</strong><br/>
                  Total stops: {tspPath.length}
                </div>
              </Popup>
            </Polyline>

            {/* Direction arrows along the path */}
            {tspPath.slice(0, -1).map((nodeId, idx) => {
              const currentNode = mapData.nodes.get(nodeId);
              const nextNode = mapData.nodes.get(tspPath[idx + 1]);

              if (!currentNode || !nextNode) return null;

              // Calculate midpoint for arrow placement
              const midLat = (currentNode.latitude + nextNode.latitude) / 2;
              const midLng = (currentNode.longitude + nextNode.longitude) / 2;

              // Calculate angle for arrow rotation
              const angle = Math.atan2(
                nextNode.latitude - currentNode.latitude,
                nextNode.longitude - currentNode.longitude
              ) * (180 / Math.PI);

              const arrowIcon = L.divIcon({
                html: `<div style="transform: rotate(${angle + 90}deg); color: ${courierColor}; font-size: 24px;">▶</div>`,
                className: 'arrow-marker',
                iconSize: [20, 20],
                iconAnchor: [10, 10]
              });

              return (
                <Marker
                  key={`arrow-${courierIdx}-${idx}`}
                  position={[midLat, midLng]}
                  icon={arrowIcon}
                />
              );
            })}

            {/* Numbers on pickup and delivery points in order of visit (not warehouse) */}
            {deliveryRequest && (() => {
              // Build a map of nodeId -> visit order (excluding warehouse which is first and last)
              // Skip first node (warehouse start) and find pickup/delivery nodes in visit order
              let visitOrder = 1;
              const nodeVisitOrder: { [key: string]: { order: number; type: string; deliveryIdx: number } } = {};

              // Go through path and assign visit order to pickup/delivery nodes only
              tspPath.forEach((nodeId, pathIdx) => {
                // Skip first node (warehouse) and last node (return to warehouse)
                if (pathIdx === 0 || pathIdx === tspPath.length - 1) return;

                // Find which delivery this node belongs to
                deliveryRequest.deliveries.forEach((delivery, deliveryIdx) => {
                  if (delivery.pickupNodeId === nodeId) {
                    nodeVisitOrder[nodeId] = { order: visitOrder++, type: 'P', deliveryIdx: deliveryIdx + 1 };
                  } else if (delivery.deliveryNodeId === nodeId) {
                    nodeVisitOrder[nodeId] = { order: visitOrder++, type: 'D', deliveryIdx: deliveryIdx + 1 };
                  }
                });
              });

              // Render markers for each node in the visit order map
              return Object.entries(nodeVisitOrder).map(([nodeId, info]) => {
                const node = mapData.nodes.get(nodeId);
                if (!node) return null;

                return (
                  <Marker
                    key={`visit-${courierIdx}-${nodeId}`}
                    position={[node.latitude, node.longitude]}
                    icon={L.divIcon({
                      html: `<div style="
                        background: ${courierColor};
                        color: white;
                        border: 2px solid #1f2937;
                        border-radius: 4px;
                        padding: 2px 6px;
                        font-weight: bold;
                        font-size: 12px;
                        box-shadow: 2px 2px 4px rgba(0,0,0,0.3);
                        white-space: nowrap;
                      ">${info.type}${info.deliveryIdx}(${info.order})</div>`,
                      className: 'visit-order-marker',
                      iconSize: [50, 20],
                      iconAnchor: [25, 10]
                    })}
                    zIndexOffset={500}
                  >
                    <Popup>
                      <strong>Courier {courierIdx + 1}</strong><br/>
                      {info.type === 'P' ? 'Pickup' : 'Delivery'} {info.deliveryIdx}<br/>
                      Visit order: {info.order}
                    </Popup>
                  </Marker>
                );
              });
            })()}
          </div>
        );
      })}

      <MapClickHandler onClick={onMapClick} />
    </MapContainer>
  );
};

export default MapComponent;
