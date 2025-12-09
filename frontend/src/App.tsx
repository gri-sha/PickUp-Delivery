import { useState, Suspense, lazy } from 'react'; // Import Suspense and lazy
import './App.css';
const MapComponent = lazy(() => import('./components/MapComponent'));
import ControlPanel from './components/ControlPanel';
import { parseMapXML, parseDeliveryXML, findNearestNode, generateDeliveryXML } from './utils/xmlParser';
import type { MapData, DeliveryRequest, CustomStop, Node } from './types';

type ClickMode = 'default' | 'setUserLocation' | 'selectPickup' | 'selectDelivery' | 'reviewNewDelivery' | 'setWarehouse' | 'collectNodes';

function App() {
  const [mapData, setMapData] = useState<MapData | null>(null);
  const [deliveryRequest, setDeliveryRequest] = useState<DeliveryRequest | null>(null);
  const [courierCount, setCourierCount] = useState<number>(1);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [clickMode, setClickMode] = useState<ClickMode>('default');
  const [clickedNodes, setClickedNodes] = useState<Node[]>([]);

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
    } else if (clickMode === 'collectNodes') {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          // Vérifier si le noeud n'est pas déjà dans la liste
          const alreadyExists = clickedNodes.some(node => node.id === nearestNode.id);
          if (!alreadyExists) {
            setClickedNodes([...clickedNodes, nearestNode]);
          } else {
            alert("Ce noeud est déjà dans la liste.");
          }
        } else {
          alert("Could not find a nearby node on the map.");
        }
      } else {
        alert("Please load a map first.");
      }
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

  const handleStartCollectNodes = () => {
    if (!mapData) {
      alert("Please load a map first.");
      return;
    }
    setClickMode('collectNodes');
  };

  const handleStopCollectNodes = () => {
    setClickMode('default');
  };

  const handleClearClickedNodes = () => {
    setClickedNodes([]);
  };

  const handleSaveClickedNodes = async () => {
    if (clickedNodes.length === 0) {
      alert("No nodes collected yet.");
      return;
    }

    const filename = prompt("Enter filename to save (e.g., clickedNodes.xml):", "clickedNodes.xml");
    if (!filename) return;

    // Générer le XML avec les noeuds cliqués
    let xmlContent = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n';
    xmlContent += '<demandeDeLivraisons>\n';

    // Premier noeud = entrepot par défaut
    xmlContent += `    <entrepot adresse="${clickedNodes[0].id}" heureDepart="8:0:0"/>\n`;

    // Les autres noeuds en paires (pickup -> delivery)
    for (let i = 1; i < clickedNodes.length; i += 2) {
      if (i + 1 < clickedNodes.length) {
        xmlContent += `    <livraison adresseEnlevement="${clickedNodes[i].id}" adresseLivraison="${clickedNodes[i + 1].id}" dureeEnlevement="180" dureeLivraison="240"/>\n`;
      }
    }

    xmlContent += '</demandeDeLivraisons>\n';

    const blob = new Blob([xmlContent], { type: "text/xml" });
    const file = new File([blob], filename, { type: "text/xml" });

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch("http://localhost:8080/upload-request", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        const result = await response.json();
        alert(`File saved successfully!\n\nFilename: ${result.filename}\nPath: ${result.path}`);
      } else {
        const errorText = await response.text();
        console.error("Upload failed:", errorText);
        alert("Failed to save request to server.");
      }
    } catch (error) {
      console.error("Error saving request:", error);
      alert("Error saving request to server.");
    }
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

  const handleSaveRequest = async () => {
    if (!deliveryRequest && customStops.length === 0) {
      alert("No delivery request to save.");
      return;
    }

    const filename = prompt("Enter filename to save (e.g., myRequest.xml):", "newRequest.xml");
    if (!filename) return;

    const xmlContent = generateDeliveryXML(deliveryRequest, customStops);
    const blob = new Blob([xmlContent], { type: "text/xml" });
    const file = new File([blob], filename, { type: "text/xml" });

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch("http://localhost:8080/upload-request", {
        method: "POST",
        body: formData,
      });

      if (response.ok) {
        const result = await response.json();
        alert(`File saved successfully!\n\nFilename: ${result.filename}\nPath: ${result.path}`);
      } else {
        const errorText = await response.text();
        console.error("Upload failed:", errorText);
        alert("Failed to save request to server.");
      }
    } catch (error) {
      console.error("Error saving request:", error);
      alert("Error saving request to server.");
    }
  };

  const handleSendRequest = () => {
    console.log("Sending request to server...");
    alert("Request sent to server!");
  };

  const handleDeleteLoadedDelivery = (index: number) => {
    if (deliveryRequest) {
      const updatedDeliveries = [...deliveryRequest.deliveries];
      updatedDeliveries.splice(index, 1);
      setDeliveryRequest({
        ...deliveryRequest,
        deliveries: updatedDeliveries
      });
    }
  };

  const handleDeleteCustomDelivery = (index: number) => {
    // Each delivery consists of 2 stops (pickup + delivery)
    // index 0 corresponds to stops 0 and 1
    // index 1 corresponds to stops 2 and 3
    const updatedStops = [...customStops];
    updatedStops.splice(index * 2, 2);
    setCustomStops(updatedStops);
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
  // Ajouter les noeuds cliqués
  clickedNodes.forEach((node, idx) => {
    displayStops.push({
      nodeId: node.id,
      latitude: node.latitude,
      longitude: node.longitude,
      type: idx === 0 ? 'warehouse' : (idx % 2 === 1 ? 'pickup' : 'delivery')
    });
  });

  return (
    <div className="app-container">
      <header>
        <h1>DeliverIF</h1>
      </header>
      
      <main>
        <div className="map-wrapper">
          <Suspense fallback={<div className="loading-map">Loading Map...</div>}>
            <MapComponent 
              mapData={mapData} 
              deliveryRequest={deliveryRequest}
              userLocation={userLocation}
              customStops={displayStops}
              onMapClick={handleMapClick}
            />
          </Suspense>
          {clickMode !== 'default' && (
            <div className="mode-indicator">
              {clickMode === 'setUserLocation' ? 'Click on map to set your location' : 
               clickMode === 'collectNodes' ? 'Click on map nodes to collect them (1st=warehouse, then pairs pickup->delivery)' :
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
          onStartCollectNodes={handleStartCollectNodes}
          onStopCollectNodes={handleStopCollectNodes}
          onSaveClickedNodes={handleSaveClickedNodes}
          onClearClickedNodes={handleClearClickedNodes}
          isCollectingNodes={clickMode === 'collectNodes'}
          clickedNodesCount={clickedNodes.length}
        />
        
        <div className="info-panel">
          <h3>Current Delivery Points</h3>
          <div className="delivery-list">
            {/* Display clicked nodes */}
            {clickedNodes.length > 0 && (
              <div className="delivery-group">
                <h4>Clicked Nodes ({clickedNodes.length})</h4>
                <ul>
                  {clickedNodes.map((node, i) => (
                    <li key={`clicked-${i}`}>
                      {i === 0 ? '🏭 Warehouse' : (i % 2 === 1 ? '📦 Pickup' : '🏠 Delivery')}: {node.id}
                    </li>
                  ))}
                </ul>
              </div>
            )}

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
                    <li key={`loaded-${i}`} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>
                        Delivery {i + 1}: Pickup ({d.pickupNodeId}, {d.pickupDuration}s) {'->'} Delivery ({d.deliveryNodeId}, {d.deliveryDuration}s)
                      </span>
                      <button 
                        onClick={() => handleDeleteLoadedDelivery(i)}
                        style={{ 
                          marginLeft: '10px', 
                          padding: '2px 8px', 
                          backgroundColor: '#fca5a5', 
                          border: '2px solid black',
                          cursor: 'pointer',
                          fontSize: '0.8rem'
                        }}
                      >
                        X
                      </button>
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
                      <li key={`custom-${i}`} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>
                          Delivery {i + 1}: Pickup ({pickup.nodeId}, {pickup.duration || 0}s) {'->'} Delivery ({delivery ? delivery.nodeId : '...'}, {delivery?.duration || 0}s)
                        </span>
                        <button 
                          onClick={() => handleDeleteCustomDelivery(i)}
                          style={{ 
                            marginLeft: '10px', 
                            padding: '2px 8px', 
                            backgroundColor: '#fca5a5', 
                            border: '2px solid black',
                            cursor: 'pointer',
                            fontSize: '0.8rem'
                          }}
                        >
                          X
                        </button>
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
