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
  customStops 
}) => {
  const center: [number, number] = [45.75, 4.85]; // Default Lyon

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

      <MapClickHandler onClick={onMapClick} />
    </MapContainer>
  );
};

export default MapComponent;
