import { useState } from 'react';
import './App.css';
import MapComponent from './components/MapComponent';
import ControlPanel from './components/ControlPanel';
import { parseMapXML, parseDeliveryXML, findNearestNode } from './utils/xmlParser';
import type { MapData, DeliveryRequest, CustomStop, Node } from './types';

type ClickMode = 'default' | 'setUserLocation' | 'selectPickup' | 'selectDelivery' | 'reviewNewDelivery' | 'setWarehouse';

function App() {
  const [mapData, setMapData] = useState<MapData | null>(null);
  const [deliveryRequest, setDeliveryRequest] = useState<DeliveryRequest | null>(null);
  const [courierCount, setCourierCount] = useState<number>(1);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [clickMode, setClickMode] = useState<ClickMode>('default');
  
  // New state for delivery creation
  const [customStops, setCustomStops] = useState<CustomStop[]>([]);
  const [tempPickupNode, setTempPickupNode] = useState<Node | null>(null);
  const [tempDeliveryNode, setTempDeliveryNode] = useState<Node | null>(null);
  const [pickupDuration, setPickupDuration] = useState<number | string>('');
  const [deliveryDuration, setDeliveryDuration] = useState<number | string>('');

  const handleLoadMap = async (xmlText: string) => {
    try {
      const data = parseMapXML(xmlText);
      setMapData(data);
    } catch (e) {
      console.error("Error parsing map XML", e);
      alert("Error parsing map XML");
    }
  };

  const handleLoadDelivery = async (xmlText: string) => {
    try {
      const data = parseDeliveryXML(xmlText);
      setDeliveryRequest(data);
    } catch (e) {
      console.error("Error parsing delivery XML", e);
      alert("Error parsing delivery XML");
    }
  };

  const handleLocateUser = () => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            lat: position.coords.latitude,
            lng: position.coords.longitude
          });
        },
        (error) => {
          console.error("Error getting location", error);
          alert("Could not get your location. Please click on the map to set it.");
          setClickMode('setUserLocation');
        }
      );
    } else {
      alert("Geolocation is not supported by this browser. Please click on the map to set it.");
      setClickMode('setUserLocation');
    }
  };

  const handleMapClick = (lat: number, lng: number) => {
    if (clickMode === 'setUserLocation') {
      setUserLocation({ lat, lng });
      setClickMode('default');
    } else if (clickMode === 'selectPickup') {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          setTempPickupNode(nearestNode);
          setClickMode('selectDelivery');
        } else {
          alert("Could not find a nearby node on the map.");
        }
      } else {
        alert("Please load a map first.");
        setClickMode('default');
      }
    } else if (clickMode === 'selectDelivery') {
      if (mapData && tempPickupNode) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          setTempDeliveryNode(nearestNode);
          setClickMode('reviewNewDelivery');
        } else {
          alert("Could not find a nearby node on the map.");
        }
      }
    } else if (clickMode === 'setWarehouse') {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          const newWarehouse = {
            nodeId: nearestNode.id,
            departureTime: deliveryRequest?.warehouse?.departureTime || "08:00:00"
          };
          
          if (deliveryRequest) {
            setDeliveryRequest({
              ...deliveryRequest,
              warehouse: newWarehouse
            });
          } else {
            setDeliveryRequest({
              warehouse: newWarehouse,
              deliveries: []
            });
          }
          setClickMode('default');
        } else {
          alert("Could not find a nearby node on the map.");
        }
      }
    } else {
      // Default behavior
      console.log(`Clicked at ${lat}, ${lng}`);
    }
  };

  const handleStartAddDelivery = () => {
    if (!mapData) {
      alert("Please load a map first.");
      return;
    }
    setClickMode('selectPickup');
    setTempPickupNode(null);
    setTempDeliveryNode(null);
    setPickupDuration('');
    setDeliveryDuration('');
  };

  const handleSetWarehouse = () => {
    if (!mapData) {
      alert("Please load a map first.");
      return;
    }
    setClickMode('setWarehouse');
  };

  const handleConfirmAdd = () => {
    if (tempPickupNode && tempDeliveryNode) {
      const newPickup: CustomStop = {
        nodeId: tempPickupNode.id,
        latitude: tempPickupNode.latitude,
        longitude: tempPickupNode.longitude,
        type: 'pickup',
        duration: Number(pickupDuration) || 0
      };
      const newDelivery: CustomStop = {
        nodeId: tempDeliveryNode.id,
        latitude: tempDeliveryNode.latitude,
        longitude: tempDeliveryNode.longitude,
        type: 'delivery',
        duration: Number(deliveryDuration) || 0
      };
      setCustomStops([...customStops, newPickup, newDelivery]);
      setTempPickupNode(null);
      setTempDeliveryNode(null);
      setPickupDuration('');
      setDeliveryDuration('');
      setClickMode('default');
    }
  };

  const handleSaveRequest = () => {
    console.log("Saving request with custom stops:", customStops);
    alert("Request saved (check console for details).");
  };

  const handleSendRequest = () => {
    console.log("Sending request to server...");
    alert("Request sent to server!");
  };

  // Helper to determine current step for ControlPanel
  const getDeliveryCreationStep = () => {
    if (clickMode === 'selectPickup') return 'select-pickup';
    if (clickMode === 'selectDelivery') return 'select-delivery';
    if (clickMode === 'reviewNewDelivery') return 'review';
    return 'idle';
  };

  // Combine custom stops with temp pickup for display
  const displayStops = [...customStops];
  if (tempPickupNode) {
    displayStops.push({
      nodeId: tempPickupNode.id,
      latitude: tempPickupNode.latitude,
      longitude: tempPickupNode.longitude,
      type: 'pickup'
    });
  }
  if (tempDeliveryNode) {
    displayStops.push({
      nodeId: tempDeliveryNode.id,
      latitude: tempDeliveryNode.latitude,
      longitude: tempDeliveryNode.longitude,
      type: 'delivery'
    });
  }

  return (
    <div className="app-container">
      <header>
        <h1>DeliverIF</h1>
      </header>
      
      <main>
        <div className="map-wrapper">
          <MapComponent 
            mapData={mapData} 
            deliveryRequest={deliveryRequest}
            userLocation={userLocation}
            customStops={displayStops}
            onMapClick={handleMapClick}
          />
          {clickMode !== 'default' && (
            <div className="mode-indicator">
              {clickMode === 'setUserLocation' ? 'Click on map to set your location' : 
               clickMode === 'selectPickup' ? 'Click on map to set Pickup point' : 
               clickMode === 'selectDelivery' ? 'Click on map to set Delivery point' :
               clickMode === 'setWarehouse' ? 'Click on map to set Warehouse location' :
               'Review points and click Confirm Add'}
              <button onClick={() => {
                setClickMode('default');
                setTempPickupNode(null);
                setTempDeliveryNode(null);
              }}>Cancel</button>
            </div>
          )}
        </div>

        <ControlPanel 
          onLoadMap={handleLoadMap}
          onLoadDelivery={handleLoadDelivery}
          courierCount={courierCount}
          setCourierCount={setCourierCount}
          onStartAddDelivery={handleStartAddDelivery}
          onConfirmAdd={handleConfirmAdd}
          onLocateUser={handleLocateUser}
          onSaveRequest={handleSaveRequest}
          onSendRequest={handleSendRequest}
          deliveryCreationStep={getDeliveryCreationStep()}
          pickupDuration={pickupDuration}
          setPickupDuration={setPickupDuration}
          deliveryDuration={deliveryDuration}
          setDeliveryDuration={setDeliveryDuration}
          onSetWarehouse={handleSetWarehouse}
          isSettingWarehouse={clickMode === 'setWarehouse'}
        />
        
        <div className="info-panel">
          <h3>Current Delivery Points</h3>
          <div className="delivery-list">
            {/* Display Warehouse */}
            {deliveryRequest && deliveryRequest.warehouse && (
              <div className="delivery-group">
                <h4>Warehouse</h4>
                <ul>
                  <li>
                    Node ID: {deliveryRequest.warehouse.nodeId} (Departure: {deliveryRequest.warehouse.departureTime})
                  </li>
                </ul>
              </div>
            )}

            {/* Display loaded deliveries */}
            {deliveryRequest && deliveryRequest.deliveries.length > 0 && (
              <div className="delivery-group">
                <h4>Loaded Deliveries</h4>
                <ul>
                  {deliveryRequest.deliveries.map((d, i) => (
                    <li key={`loaded-${i}`}>
                      Delivery {i + 1}: Pickup ({d.pickupNodeId}, {d.pickupDuration}s) {'->'} Delivery ({d.deliveryNodeId}, {d.deliveryDuration}s)
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Display custom deliveries */}
            {customStops.length > 0 && (
              <div className="delivery-group">
                <h4>New Deliveries</h4>
                <ul>
                  {Array.from({ length: Math.ceil(customStops.length / 2) }).map((_, i) => {
                    const pickup = customStops[i * 2];
                    const delivery = customStops[i * 2 + 1];
                    return (
                      <li key={`custom-${i}`}>
                        Delivery {i + 1}: Pickup ({pickup.nodeId}, {pickup.duration || 0}s) {'->'} Delivery ({delivery ? delivery.nodeId : '...'}, {delivery?.duration || 0}s)
                      </li>
                    );
                  })}
                </ul>
              </div>
            )}
            
            {!deliveryRequest && customStops.length === 0 && (
              <p style={{ color: '#6b7280', fontStyle: 'italic' }}>No deliveries loaded or added yet.</p>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
