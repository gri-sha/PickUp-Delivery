import { useState, Suspense, lazy } from "react"; // Import Suspense and lazy
import "./App.css";
const MapComponent = lazy(() => import("./components/MapComponent"));
import ControlPanel from "./components/ControlPanel";
import {
  parseMapXML,
  parseDeliveryXML,
  findNearestNode,
  generateDeliveryXML,
} from "./utils/xmlParser";
import type {
  MapData,
  DeliveryRequest,
  CustomStop,
  Node,
  TspPath,
} from "./types";

type ClickMode =
  | "default"
  | "setUserLocation"
  | "selectPickup"
  | "selectDelivery"
  | "reviewNewDelivery"
  | "setWarehouse"
  | "collectNodes";

function App() {
  const [mapData, setMapData] = useState<MapData | null>(null);
  const [deliveryRequest, setDeliveryRequest] =
    useState<DeliveryRequest | null>(null);
  const [courierCount, setCourierCount] = useState<number>(1);
  const [userLocation, setUserLocation] = useState<{
    lat: number;
    lng: number;
  } | null>(null);
  const [clickMode, setClickMode] = useState<ClickMode>("default");
  const [clickedNodes, setClickedNodes] = useState<Node[]>([]);
  const [tspPaths, setTspPaths] = useState<string[][]>([]); // Array of paths, one per courier
  const [nbCouriers, setNbCouriers] = useState<number>(0);
  const [nbDeliveries, setNbDeliveries] = useState<number>(0);

  // New state for delivery creation
  const [customStops, setCustomStops] = useState<CustomStop[]>([]);
  const [tempPickupNode, setTempPickupNode] = useState<Node | null>(null);
  const [tempDeliveryNode, setTempDeliveryNode] = useState<Node | null>(null);
  const [pickupDuration, setPickupDuration] = useState<number | string>("");
  const [deliveryDuration, setDeliveryDuration] = useState<number | string>("");

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
            lng: position.coords.longitude,
          });
        },
        (error) => {
          console.error("Error getting location", error);
          alert(
            "Could not get your location. Please click on the map to set it."
          );
          setClickMode("setUserLocation");
        }
      );
    } else {
      alert(
        "Geolocation is not supported by this browser. Please click on the map to set it."
      );
      setClickMode("setUserLocation");
    }
  };

  const handleMapClick = (lat: number, lng: number) => {
    if (clickMode === "setUserLocation") {
      setUserLocation({ lat, lng });
      setClickMode("default");
    } else if (clickMode === "collectNodes") {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          const alreadyExists = clickedNodes.some(
            (node) => node.id === nearestNode.id
          );
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
    } else if (clickMode === "selectPickup") {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          setTempPickupNode(nearestNode);
          setClickMode("selectDelivery");
        } else {
          alert("Could not find a nearby node on the map.");
        }
      } else {
        alert("Please load a map first.");
        setClickMode("default");
      }
    } else if (clickMode === "selectDelivery") {
      if (mapData && tempPickupNode) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          setTempDeliveryNode(nearestNode);
          setClickMode("reviewNewDelivery");
        } else {
          alert("Could not find a nearby node on the map.");
        }
      }
    } else if (clickMode === "setWarehouse") {
      if (mapData) {
        const nearestNode = findNearestNode(mapData, lat, lng);
        if (nearestNode) {
          const newWarehouse = {
            nodeId: nearestNode.id,
            departureTime:
              deliveryRequest?.warehouse?.departureTime || "08:00:00",
          };

          if (deliveryRequest) {
            setDeliveryRequest({
              ...deliveryRequest,
              warehouse: newWarehouse,
            });
          } else {
            setDeliveryRequest({
              warehouse: newWarehouse,
              deliveries: [],
            });
          }
          setClickMode("default");
        } else {
          alert("Could not find a nearby node on the map.");
        }
      }
    } else {
      console.log(`Clicked at ${lat}, ${lng}`);
    }
  };

  const handleConfirmAdd = () => {
    if (tempPickupNode && tempDeliveryNode) {
      const newPickup: CustomStop = {
        nodeId: tempPickupNode.id,
        latitude: tempPickupNode.latitude,
        longitude: tempPickupNode.longitude,
        type: "pickup",
        duration: Number(pickupDuration) || 0,
      };
      const newDelivery: CustomStop = {
        nodeId: tempDeliveryNode.id,
        latitude: tempDeliveryNode.latitude,
        longitude: tempDeliveryNode.longitude,
        type: "delivery",
        duration: Number(deliveryDuration) || 0,
      };
      setCustomStops([...customStops, newPickup, newDelivery]);
      setTempPickupNode(null);
      setTempDeliveryNode(null);
      setPickupDuration("");
      setDeliveryDuration("");
      setClickMode("default");
    }
  };

  const handleSaveRequest = async () => {
    if (!deliveryRequest && customStops.length === 0) {
      alert("No delivery request to save.");
      return;
    }

    const filename = prompt(
      "Enter filename to save (e.g., myRequest.xml):",
      "newRequest.xml"
    );
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
        alert(
          `File saved successfully!\n\nFilename: ${result.filename}\nPath: ${result.path}`
        );
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

  const handleStartAddDelivery = () => {
    if (!mapData) {
      alert("Please load a map first.");
      return;
    }
    setClickMode("selectPickup");
    setTempPickupNode(null);
    setTempDeliveryNode(null);
    setPickupDuration("");
    setDeliveryDuration("");
  };

  const handleSetWarehouse = () => {
    if (!mapData) {
      alert("Please load a map first.");
      return;
    }
    setClickMode("setWarehouse");
  };

  const handleStartCollectNodes = () => {
    if (!mapData) {
      alert("Please load a map first.");
      return;
    }

    // Clear previous clicked nodes when starting a new collection
    setClickedNodes([]);

    // Inform user about the behavior
    if (deliveryRequest?.warehouse) {
      alert(
        "Click & Collect Mode:\n\n" +
          "✓ Warehouse from loaded request will be kept\n" +
          "✓ Existing deliveries from loaded request will be kept\n" +
          "✓ Click nodes on map to add NEW deliveries (pairs: pickup → delivery)\n" +
          "✓ Save will merge everything into one XML file"
      );
    } else {
      alert(
        "Click & Collect Mode:\n\n" +
          "✓ First click = Warehouse\n" +
          "✓ Then click pairs: pickup → delivery\n" +
          "✓ Save will create a new delivery request XML"
      );
    }

    setClickMode("collectNodes");
  };

  const handleStopCollectNodes = () => {
    setClickMode("default");
  };

  const handleClearClickedNodes = () => {
    setClickedNodes([]);
  };

  const handleSaveClickedNodes = async () => {
    if (
      clickedNodes.length === 0 &&
      (!deliveryRequest || deliveryRequest.deliveries.length === 0)
    ) {
      alert("No nodes collected and no delivery request loaded.");
      return;
    }

    const filename = prompt(
      "Enter filename to save (e.g., clickedNodes.xml):",
      "clickedNodes.xml"
    );
    if (!filename) return;

    // Générer le XML en fusionnant la demande chargée (si présente) avec les noeuds cliqués
    let xmlContent = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n';
    xmlContent += "<demandeDeLivraisons>\n";

    // 1. ENTREPÔT
    // Si une demande est chargée avec un entrepôt, on le réutilise
    // Sinon, le premier nœud cliqué devient l'entrepôt
    if (deliveryRequest?.warehouse) {
      const departureTime = deliveryRequest.warehouse.departureTime || "8:0:0";
      xmlContent += `    <entrepot adresse="${deliveryRequest.warehouse.nodeId}" heureDepart="${departureTime}"/>\n`;
    } else if (clickedNodes.length > 0) {
      xmlContent += `    <entrepot adresse="${clickedNodes[0].id}" heureDepart="8:0:0"/>\n`;
    } else {
      alert(
        "No warehouse defined (no delivery request loaded and no nodes clicked)."
      );
      return;
    }

    // 2. LIVRAISONS EXISTANTES (de la demande chargée)
    if (deliveryRequest?.deliveries?.length) {
      for (const d of deliveryRequest.deliveries) {
        xmlContent += `    <livraison adresseEnlevement="${d.pickupNodeId}" adresseLivraison="${d.deliveryNodeId}" dureeEnlevement="${d.pickupDuration}" dureeLivraison="${d.deliveryDuration}"/>\n`;
      }
    }

    // 3. NOUVELLES LIVRAISONS (à partir des nœuds cliqués)
    // Si une demande est déjà chargée avec entrepôt, tous les clics sont des paires pickup/delivery
    // Sinon, on saute le 1er clic (qui est l'entrepôt) et on commence à partir du 2ème
    const startIndex = deliveryRequest?.warehouse ? 0 : 1;

    for (let i = startIndex; i < clickedNodes.length; i += 2) {
      if (i + 1 < clickedNodes.length) {
        xmlContent += `    <livraison adresseEnlevement="${
          clickedNodes[i].id
        }" adresseLivraison="${
          clickedNodes[i + 1].id
        }" dureeEnlevement="180" dureeLivraison="240"/>\n`;
      }
    }

    xmlContent += "</demandeDeLivraisons>\n";

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
        const totalDeliveries =
          (deliveryRequest?.deliveries?.length || 0) +
          Math.floor((clickedNodes.length - startIndex) / 2);
        alert(
          `File saved successfully!\n\nFilename: ${result.filename}\nPath: ${
            result.path
          }\n\nTotal deliveries saved: ${totalDeliveries}\n- From loaded request: ${
            deliveryRequest?.deliveries?.length || 0
          }\n- From clicked nodes: ${Math.floor(
            (clickedNodes.length - startIndex) / 2
          )}`
        );
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

  const handleComputeTsp = async (
    planName: string,
    requestName: string,
    nDrivers: number,
    speedFactor: number,
    maxSeconds: number
  ) => {
    if (!planName || !requestName) {
      alert("Please select both a plan and a request before calculating TSP.");
      return;
    }

    try {
      const params = new URLSearchParams({
        planName,
        requestName,
        nDrivers: String(nDrivers),
        speedFactor: String(speedFactor),
        maxSeconds: String(maxSeconds),
      });
      const response = await fetch(
        `http://localhost:8080/get-tsp?${params.toString()}`
      );
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Failed to fetch TSP");
      }

      // Backend now returns {paths: number[][], nbCouriers: number, nbDeliveries: number}
      const data: {
        paths: number[][];
        nbCouriers: number;
        nbDeliveries: number;
      } = await response.json();
      console.log("TSP response:", data);

      if (!data.paths || data.paths.length === 0) {
        throw new Error("No TSP paths returned");
      }

      // Convert all paths to string arrays
      const allPathsStr = data.paths.map((path) =>
        path.map((id) => id.toString())
      );
      setTspPaths(allPathsStr);
      setNbCouriers(data.nbCouriers);
      setNbDeliveries(data.nbDeliveries);

      alert(
        `TSP computed successfully!\n${data.nbDeliveries} deliveries\n${data.nbCouriers} courier(s) needed`
      );
    } catch (error: any) {
      console.error("Error computing TSP:", error);
      alert(`Failed to compute TSP: ${error.message || "Unknown error"}`);
    }
  };

  const handleUploadAndComputeTsp = async (
    planFile: File,
    requestFile: File,
    nDrivers: number,
    speedFactor: number,
    maxSeconds: number
  ) => {
    try {
      const formData = new FormData();
      formData.append("plan", planFile);
      formData.append("request", requestFile);
      formData.append("nDrivers", String(nDrivers));
      formData.append("speedFactor", String(speedFactor));
      formData.append("maxSeconds", String(maxSeconds));

      const response = await fetch("http://localhost:8080/get-tsp", {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Failed to compute TSP");
      }

      // Backend now returns {paths: number[][], nbCouriers: number, nbDeliveries: number}
      const data: {
        paths: number[][];
        nbCouriers: number;
        nbDeliveries: number;
      } = await response.json();
      console.log("TSP response:", data);

      if (!data.paths || data.paths.length === 0) {
        throw new Error("No TSP paths returned");
      }

      // Convert all paths to string arrays
      const allPathsStr = data.paths.map((path) =>
        path.map((id) => id.toString())
      );
      setTspPaths(allPathsStr);
      setNbCouriers(data.nbCouriers);
      setNbDeliveries(data.nbDeliveries);

      // Load and parse the uploaded files to display on map
      const planText = await planFile.text();
      const requestText = await requestFile.text();
      await handleLoadMap(planText);
      await handleLoadDelivery(requestText);

      alert(
        `TSP computed successfully!\n${data.nbDeliveries} deliveries\n${data.nbCouriers} courier(s) needed`
      );
    } catch (error: any) {
      console.error("Error uploading and computing TSP:", error);
      alert(`Failed to compute TSP: ${error.message || "Unknown error"}`);
    }
  };

  const handleDeleteLoadedDelivery = (index: number) => {
    if (!deliveryRequest) return;
    const updated = [...deliveryRequest.deliveries];
    updated.splice(index, 1);
    setDeliveryRequest({
      ...deliveryRequest,
      deliveries: updated,
    });
  };

  const handleDeleteCustomDelivery = (index: number) => {
    const updated = [...customStops];
    // each custom delivery = 2 stops (pickup + delivery)
    updated.splice(index * 2, 2);
    setCustomStops(updated);
  };

  // Helper to determine current step for ControlPanel
  const getDeliveryCreationStep = () => {
    if (clickMode === "selectPickup") return "select-pickup";
    if (clickMode === "selectDelivery") return "select-delivery";
    if (clickMode === "reviewNewDelivery") return "review";
    return "idle";
  };

  // Combine custom stops with temp pickup for display
  const displayStops = [...customStops];
  if (tempPickupNode) {
    displayStops.push({
      nodeId: tempPickupNode.id,
      latitude: tempPickupNode.latitude,
      longitude: tempPickupNode.longitude,
      type: "pickup",
    });
  }
  if (tempDeliveryNode) {
    displayStops.push({
      nodeId: tempDeliveryNode.id,
      latitude: tempDeliveryNode.latitude,
      longitude: tempDeliveryNode.longitude,
      type: "delivery",
    });
  }
  // Ajouter les noeuds cliqués
  // Si une demande est chargée avec entrepôt, tous les clics sont pickup/delivery
  // Sinon, le 1er clic est l'entrepôt
  clickedNodes.forEach((node, idx) => {
    let nodeType: "warehouse" | "pickup" | "delivery";

    if (deliveryRequest?.warehouse) {
      // Si warehouse existe déjà, tous les clics sont des paires pickup/delivery
      nodeType = idx % 2 === 0 ? "pickup" : "delivery";
    } else {
      // Sinon, 1er clic = warehouse, puis paires pickup/delivery
      nodeType =
        idx === 0 ? "warehouse" : idx % 2 === 1 ? "pickup" : "delivery";
    }

    displayStops.push({
      nodeId: node.id,
      latitude: node.latitude,
      longitude: node.longitude,
      type: nodeType,
    });
  });

  return (
    <div className="app-container">
      <header>
        <h1>DeliverIF</h1>
      </header>

      <main>
        <div className="map-wrapper">
          <Suspense
            fallback={<div className="loading-map">Loading Map...</div>}
          >
            <MapComponent
              mapData={mapData}
              deliveryRequest={deliveryRequest}
              userLocation={userLocation}
              customStops={displayStops}
              onMapClick={handleMapClick}
              tspPaths={tspPaths}
            />
          </Suspense>
          {clickMode !== "default" && (
            <div className="mode-indicator">
              {clickMode === "setUserLocation"
                ? "Click on map to set your location"
                : clickMode === "collectNodes"
                ? "Click on map nodes to collect them (1st=warehouse, then pairs pickup->delivery)"
                : clickMode === "selectPickup"
                ? "Click on map to set Pickup point"
                : clickMode === "selectDelivery"
                ? "Click on map to set Delivery point"
                : clickMode === "setWarehouse"
                ? "Click on map to set Warehouse location"
                : "Review points and click Confirm Add"}
              <button
                onClick={() => {
                  setClickMode("default");
                  setTempPickupNode(null);
                  setTempDeliveryNode(null);
                }}
              >
                Cancel
              </button>
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
          deliveryCreationStep={getDeliveryCreationStep()}
          pickupDuration={pickupDuration}
          setPickupDuration={setPickupDuration}
          deliveryDuration={deliveryDuration}
          setDeliveryDuration={setDeliveryDuration}
          onStartCollectNodes={handleStartCollectNodes}
          onStopCollectNodes={handleStopCollectNodes}
          onSaveClickedNodes={handleSaveClickedNodes}
          onClearClickedNodes={handleClearClickedNodes}
          isCollectingNodes={clickMode === "collectNodes"}
          clickedNodesCount={clickedNodes.length}
          onComputeTsp={handleComputeTsp}
          onUploadAndComputeTsp={handleUploadAndComputeTsp}
        />

        <div className="info-panel">
          <h3>Current Delivery Points</h3>
          <div className="delivery-list">
            {/* Display TSP info */}
            {nbCouriers > 0 && (
              <div
                className="delivery-group"
                style={{
                  backgroundColor: "#e0f2fe",
                  padding: "10px",
                  borderRadius: "5px",
                  marginBottom: "10px",
                }}
              >
                <h4 style={{ color: "#0369a1", marginTop: 0 }}>
                  🚚 TSP Solution
                </h4>
                <p style={{ margin: "5px 0", fontWeight: "bold" }}>
                  {nbDeliveries} deliveries → {nbCouriers} courier
                  {nbCouriers > 1 ? "s" : ""} needed
                </p>
                <p
                  style={{
                    margin: "5px 0",
                    fontSize: "0.85rem",
                    color: "#075985",
                  }}
                >
                  {tspPaths.map((path, i) => (
                    <span key={i}>
                      Courier {i + 1}: {path.length} stops
                      {i < tspPaths.length - 1 ? " | " : ""}
                    </span>
                  ))}
                </p>
              </div>
            )}

            {/* Display clicked nodes */}
            {clickedNodes.length > 0 && (
              <div className="delivery-group">
                <h4>Clicked Nodes ({clickedNodes.length})</h4>
                <ul>
                  {clickedNodes.map((node, i) => (
                    <li key={`clicked-${i}`}>
                      {i === 0
                        ? "🏭 Warehouse"
                        : i % 2 === 1
                        ? "📦 Pickup"
                        : "🏠 Delivery"}
                      : {node.id}
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
                    Node ID: {deliveryRequest.warehouse.nodeId} (Departure:{" "}
                    {deliveryRequest.warehouse.departureTime})
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
                    <li
                      key={`loaded-${i}`}
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                      }}
                    >
                      <span>
                        Delivery {i + 1}: Pickup ({d.pickupNodeId},{" "}
                        {d.pickupDuration}s) {"->"} Delivery ({d.deliveryNodeId}
                        , {d.deliveryDuration}s)
                      </span>
                      <button
                        onClick={() => handleDeleteLoadedDelivery(i)}
                        style={{
                          marginLeft: "10px",
                          padding: "2px 8px",
                          backgroundColor: "#fca5a5",
                          border: "2px solid black",
                          cursor: "pointer",
                          fontSize: "0.8rem",
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
                  {Array.from({
                    length: Math.ceil(customStops.length / 2),
                  }).map((_, i) => {
                    const pickup = customStops[i * 2];
                    const delivery = customStops[i * 2 + 1];
                    return (
                      <li
                        key={`custom-${i}`}
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                        }}
                      >
                        <span>
                          Delivery {i + 1}: Pickup ({pickup.nodeId},{" "}
                          {pickup.duration || 0}s) {"->"} Delivery (
                          {delivery ? delivery.nodeId : "..."},{" "}
                          {delivery?.duration || 0}s)
                        </span>
                        <button
                          onClick={() => handleDeleteCustomDelivery(i)}
                          style={{
                            marginLeft: "10px",
                            padding: "2px 8px",
                            backgroundColor: "#fca5a5",
                            border: "2px solid black",
                            cursor: "pointer",
                            fontSize: "0.8rem",
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
              <p style={{ color: "#6b7280", fontStyle: "italic" }}>
                No deliveries loaded or added yet.
              </p>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;
