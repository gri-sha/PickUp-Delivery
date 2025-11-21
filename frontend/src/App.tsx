import { useState } from 'react';
import './App.css';
import MapComponent from './components/MapComponent';
import ControlPanel from './components/ControlPanel';
import { parseMapXML, parseDeliveryXML, findNearestNode } from './utils/xmlParser';
import type { MapData, DeliveryRequest, CustomStop, StopType } from './types';

type ClickMode = 'default' | 'setUserLocation' | 'addStop';

function App() {
  const [mapData, setMapData] = useState<MapData | null>(null);
  const [deliveryRequest, setDeliveryRequest] = useState<DeliveryRequest | null>(null);
  const [courierCount, setCourierCount] = useState<number>(1);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [clickMode, setClickMode] = useState<ClickMode>('default');
  const [customStops, setCustomStops] = useState<CustomStop[]>([]);
  const [pendingStopType, setPendingStopType] = useState<StopType>('pickup');

  const handleLoadMap = async (file: File) => {
    const text = await file.text();
    try {
      const data = parseMapXML(text);
      setMapData(data);
    } catch (e) {
      console.error("Error parsing map XML", e);
      alert("Error parsing map XML");
    }
  };

  const handleLoadDelivery = async (file: File) => {
    const text = await file.text();
    try {
      const data = parseDeliveryXML(text);
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
    } else if (clickMode === 'addStop') {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          setCustomStops([...customStops, { 
            nodeId: nearestNode.id, 
            latitude: nearestNode.latitude, 
            longitude: nearestNode.longitude,
            type: pendingStopType
          }]);
        } else {
          alert("Could not find a nearby node on the map.");
        }
      } else {
        alert("Please load a map first.");
      }
      setClickMode('default');
    } else {
      // Default behavior (maybe select a node?)
      console.log(`Clicked at ${lat}, ${lng}`);
    }
  };

  const handleAddStop = (type: StopType) => {
    setPendingStopType(type);
    setClickMode('addStop');
  };

  const handleSaveRequest = () => {
    // In a real app, this would merge customStops into deliveryRequest and maybe download XML
    console.log("Saving request with custom stops:", customStops);
    alert("Request saved (check console for details).");
  };

  const handleSendRequest = () => {
    console.log("Sending request to server...");
    alert("Request sent to server!");
  };

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
            customStops={customStops}
            onMapClick={handleMapClick}
          />
          {clickMode !== 'default' && (
            <div className="mode-indicator">
              Click on map to {clickMode === 'setUserLocation' ? 'set your location' : `add a ${pendingStopType}`}
              <button onClick={() => setClickMode('default')}>Cancel</button>
            </div>
          )}
        </div>

        <ControlPanel 
          onLoadMap={handleLoadMap}
          onLoadDelivery={handleLoadDelivery}
          courierCount={courierCount}
          setCourierCount={setCourierCount}
          onAddStop={handleAddStop}
          onLocateUser={handleLocateUser}
          onSaveRequest={handleSaveRequest}
          onSendRequest={handleSendRequest}
        />
        
        <div className="info-panel">
          {customStops.length > 0 && (
            <div>
              <h3>Custom Stops:</h3>
              <ul>
                {customStops.map((stop, idx) => (
                  <li key={idx}>{stop.type} at {stop.latitude.toFixed(4)}, {stop.longitude.toFixed(4)} (Node: {stop.nodeId})</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

export default App;
