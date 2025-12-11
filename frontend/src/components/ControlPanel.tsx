import React, { useState, useEffect, useCallback } from "react";
import { RefreshCw } from "lucide-react";

interface ControlPanelProps {
  onLoadMap: (xmlText: string) => void;
  onLoadDelivery: (xmlText: string) => void;
  courierCount: number;
  setCourierCount: (count: number) => void;
  onStartAddDelivery: () => void;
  onConfirmAdd: () => void;
  onLocateUser: () => void;
  onSaveRequest: () => void;
  deliveryCreationStep: "idle" | "select-pickup" | "select-delivery" | "review";
  pickupDuration: number | string;
  setPickupDuration: (duration: number | string) => void;
  deliveryDuration: number | string;
  setDeliveryDuration: (duration: number | string) => void;
  onSetWarehouse: () => void;
  isSettingWarehouse: boolean;
  onStartCollectNodes: () => void;
  onStopCollectNodes: () => void;
  onSaveClickedNodes: () => void;
  onClearClickedNodes: () => void;
  isCollectingNodes: boolean;
  clickedNodesCount: number;
  onComputeTsp: () => void;
}

export default function ControlPanel({
  onLoadMap,
  onLoadDelivery,
  courierCount,
  setCourierCount,
  onStartAddDelivery,
  onConfirmAdd,
  onLocateUser,
  onSaveRequest,
  deliveryCreationStep,
  pickupDuration,
  setPickupDuration,
  deliveryDuration,
  setDeliveryDuration,
  onSetWarehouse,
  isSettingWarehouse,
  onStartCollectNodes,
  onStopCollectNodes,
  onSaveClickedNodes,
  onClearClickedNodes,
  isCollectingNodes,
  clickedNodesCount,
  onComputeTsp,
}: ControlPanelProps) {
  const [MAP_FILES, setMapFiles] = useState<string[]>([]);
  const [REQUEST_FILES, setRequestFiles] = useState<string[]>([]);

  const [selectedMap, setSelectedMap] = useState<string>("");
  const [selectedRequest, setSelectedRequest] = useState<string>("");

  const [planFile, setPlanFile] = useState<File | null>(null);
  const [requestFile, setRequestFile] = useState<File | null>(null);

  const uploadXmlAndComputeTsp = async () => {
    if (!planFile || !requestFile) {
      alert("Please select both Plan XML and Request XML files.");
      return;
    }
    try {
      const form = new FormData();
      console.log("Preparing files for upload:", planFile, requestFile);
      form.append("plan", planFile);
      form.append("request", requestFile);
      for (const pair of form.entries()) {
        console.log(pair[0], pair[1]); // pair[0] = nom du champ, pair[1] = fichier
      }
      const resp = await fetch("http://localhost:8080/get-tsp", {
        method: "POST",
        body: form,
      });
      if (!resp.ok) {
        const text = await resp.text();
        throw new Error(text || `HTTP ${resp.status}`);
      }
      const pathIds: number[] = await resp.json();
      console.log("Computed TSP path ids:", pathIds);
      alert(`TSP computed. Path length: ${pathIds.length}`);
    } catch (e: unknown) {
      console.error("Upload/Compute TSP failed", e);
      alert(`Upload/Compute failed: ${e instanceof Error ? e.message : String(e)}`);
    }
  };

  const handleMapSelect = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const filename = e.target.value;
    setSelectedMap(filename);

    if (filename) {
      try {
        console.log(`Fetching map file: ${filename}`);
        const response = await fetch(`http://localhost:8080/plans/${filename}`);

        const text = await response.text();
        onLoadMap(text); // Tu reçois le contenu XML renvoyé par Spring
      } catch (error) {
        console.error("Error loading map:", error);
        alert("Failed to load map file.");
      }
    }
  };

  const handleRequestSelect = async (
    e: React.ChangeEvent<HTMLSelectElement>
  ) => {
    const filename = e.target.value;
    setSelectedRequest(filename);
    if (filename) {
      try {
        const response = await fetch(
          `http://localhost:8080/requests/${filename}`
        );
        const text = await response.text();
        onLoadDelivery(text);
      } catch (error) {
        console.error("Error loading request:", error);
        alert("Failed to load request file.");
      }
    }
  };

  const fetchMapFiles = useCallback(async () => {
    try {
      const response = await fetch("http://localhost:8080/plan-names");
      const data = await response.json();
      setMapFiles(data);
    } catch (error) {
      console.error("Error fetching map files:", error);
    }
  }, []);

  const fetchRequestFiles = useCallback(async () => {
    try {
      const response = await fetch("http://localhost:8080/request-names");
      const data = await response.json();
      setRequestFiles(data);
    } catch (error) {
      console.error("Error fetching request files:", error);
    }
  }, []);

  useEffect(() => {
    const loadData = async () => {
      await fetchRequestFiles();
      await fetchMapFiles();
    };
    loadData();
  }, [fetchMapFiles, fetchRequestFiles]);

  return (
    <div className="control-panel">
      {/* Left Column: Configuration */}
      <div className="control-section">
        <h3>Configuration</h3>

        <div>
          <button onClick={onLocateUser} style={{ width: "100%" }}>
            Locate Me
          </button>
        </div>

        <div>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "5px",
            }}
          >
            <label>Select Map:</label>
            <button
              onClick={fetchMapFiles}
              style={{
                padding: "4px 8px",
                fontSize: "0.8rem",
                display: "flex",
                alignItems: "center",
                gap: "4px",
                cursor: "pointer",
              }}
              title="Refresh Map List"
            >
              <RefreshCw size={12} />
            </button>
          </div>
          <select
            value={selectedMap}
            onChange={handleMapSelect}
            style={{
              width: "100%",
              padding: "8px",
              border: "2px solid #1f2937",
            }}
          >
            <option value="">-- Select a Map --</option>
            {MAP_FILES.map((file) => (
              <option key={file} value={file}>
                {file}
              </option>
            ))}
          </select>
        </div>

        <div>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "5px",
            }}
          >
            <label>Select Delivery Request:</label>
            <button
              onClick={fetchRequestFiles}
              style={{
                padding: "4px 8px",
                fontSize: "0.8rem",
                display: "flex",
                alignItems: "center",
                gap: "4px",
                cursor: "pointer",
              }}
              title="Refresh Request List"
            >
              <RefreshCw size={12} />
            </button>
          </div>
          <select
            value={selectedRequest}
            onChange={handleRequestSelect}
            style={{
              width: "100%",
              padding: "8px",
              border: "2px solid #1f2937",
            }}
          >
            <option value="">-- Select a Request --</option>
            {REQUEST_FILES.map((file) => (
              <option key={file} value={file}>
                {file}
              </option>
            ))}
          </select>
        </div>

        {/* Local XML upload to compute TSP on backend */}
        <div
          style={{
            border: "2px solid #1f2937",
            padding: "8px",
            marginTop: "10px",
          }}
        >
          <h4 style={{ marginTop: 0 }}>Upload XMLs (Plan + Request)</h4>
          <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <div>
              <label style={{ fontSize: "0.8rem" }}>Plan XML:</label>
              <input
                type="file"
                accept=".xml"
                onChange={(e) => setPlanFile(e.target.files?.[0] || null)}
              />
            </div>
            <div>
              <label style={{ fontSize: "0.8rem" }}>Request XML:</label>
              <input
                type="file"
                accept=".xml"
                onChange={(e) => setRequestFile(e.target.files?.[0] || null)}
              />
            </div>
            <button
              onClick={uploadXmlAndComputeTsp}
              disabled={!planFile || !requestFile}
            >
              Upload & Compute TSP
            </button>
          </div>
        </div>

        <div>
          <label>Number of Couriers: </label>
          <input
            type="number"
            min="1"
            value={courierCount}
            onChange={(e) => setCourierCount(parseInt(e.target.value))}
          />
        </div>
      </div>

      {/* Right Column: Actions */}
      <div className="control-section">
        <h3>Actions</h3>

        <div>
          <button
            onClick={onSetWarehouse}
            style={{
              width: "100%",
              marginBottom: "10px",
              backgroundColor: isSettingWarehouse ? "#fde047" : "#ffffff",
            }}
          >
            {isSettingWarehouse
              ? "Click on Map to Set Warehouse"
              : "Set Warehouse Location"}
          </button>

          {/* Section Collect Nodes */}
          <div
            style={{
              border: "2px solid #1f2937",
              padding: "10px",
              marginBottom: "10px",
              backgroundColor: isCollectingNodes ? "#fef3c7" : "#f9fafb",
            }}
          >
            <h4
              style={{ marginTop: 0, marginBottom: "10px", fontSize: "0.9rem" }}
            >
              Click & Collect Nodes
            </h4>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#6b7280",
                marginBottom: "10px",
              }}
            >
              {isCollectingNodes
                ? `Collected: ${clickedNodesCount} nodes (1st=warehouse, then pairs)`
                : "Click nodes on map to create delivery request"}
            </p>
            <div style={{ display: "flex", gap: "5px", marginBottom: "5px" }}>
              {!isCollectingNodes ? (
                <button
                  onClick={onStartCollectNodes}
                  style={{ flex: 1, backgroundColor: "#86efac" }}
                >
                  Start Collecting
                </button>
              ) : (
                <button
                  onClick={onStopCollectNodes}
                  style={{ flex: 1, backgroundColor: "#fca5a5" }}
                >
                  Stop
                </button>
              )}
            </div>
            <div style={{ display: "flex", gap: "5px" }}>
              <button
                onClick={onClearClickedNodes}
                disabled={clickedNodesCount === 0}
                style={{
                  flex: 1,
                  fontSize: "0.8rem",
                  opacity: clickedNodesCount === 0 ? 0.5 : 1,
                  cursor: clickedNodesCount === 0 ? "not-allowed" : "pointer",
                }}
              >
                Clear
              </button>
              <button
                onClick={onSaveClickedNodes}
                disabled={clickedNodesCount === 0}
                style={{
                  flex: 1,
                  fontSize: "0.8rem",
                  backgroundColor:
                    clickedNodesCount > 0 ? "#93c5fd" : "#e5e7eb",
                  opacity: clickedNodesCount === 0 ? 0.5 : 1,
                  cursor: clickedNodesCount === 0 ? "not-allowed" : "pointer",
                }}
              >
                Save as XML
              </button>
            </div>
          </div>

          <button
            onClick={
              deliveryCreationStep === "review"
                ? onConfirmAdd
                : onStartAddDelivery
            }
            disabled={
              deliveryCreationStep === "select-pickup" ||
              deliveryCreationStep === "select-delivery" ||
              (deliveryCreationStep === "review" &&
                (pickupDuration === "" || deliveryDuration === ""))
            }
            style={{
              width: "100%",
              backgroundColor:
                deliveryCreationStep === "select-pickup" ||
                deliveryCreationStep === "select-delivery" ||
                (deliveryCreationStep === "review" &&
                  (pickupDuration === "" || deliveryDuration === ""))
                  ? "#e5e7eb"
                  : deliveryCreationStep === "review"
                  ? "#86efac"
                  : "#ffffff",
              cursor:
                deliveryCreationStep === "select-pickup" ||
                deliveryCreationStep === "select-delivery" ||
                (deliveryCreationStep === "review" &&
                  (pickupDuration === "" || deliveryDuration === ""))
                  ? "not-allowed"
                  : "pointer",
            }}
          >
            {deliveryCreationStep === "idle"
              ? "Add New Delivery"
              : deliveryCreationStep === "select-pickup"
              ? "Select Pickup Point on Map..."
              : deliveryCreationStep === "select-delivery"
              ? "Select Delivery Point on Map..."
              : "Confirm Add"}
          </button>
          {deliveryCreationStep !== "idle" && (
            <div style={{ marginTop: "10px" }}>
              <p
                style={{
                  fontSize: "0.8rem",
                  marginBottom: "10px",
                  color: "#6b7280",
                }}
              >
                {deliveryCreationStep === "select-pickup"
                  ? "Click on the map to set the pickup location."
                  : deliveryCreationStep === "select-delivery"
                  ? "Click on the map to set the delivery location."
                  : "Review the points on the map and click Confirm Add."}
              </p>

              <div
                style={{
                  display: "flex",
                  gap: "10px",
                  flexDirection: "column",
                }}
              >
                <div>
                  <label style={{ fontSize: "0.8rem" }}>
                    Pickup Duration (sec) *:
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={pickupDuration}
                    onChange={(e) => setPickupDuration(e.target.value)}
                    style={{ width: "100%" }}
                    required
                    placeholder="Enter duration"
                  />
                </div>
                <div>
                  <label style={{ fontSize: "0.8rem" }}>
                    Delivery Duration (sec) *:
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={deliveryDuration}
                    onChange={(e) => setDeliveryDuration(e.target.value)}
                    style={{ width: "100%" }}
                    required
                    placeholder="Enter duration"
                  />
                </div>
              </div>
            </div>
          )}
        </div>

        <div style={{ display: "flex", gap: "10px", marginTop: "auto" }}>
          <button onClick={onSaveRequest} style={{ flex: 1 }}>
            Save Request
          </button>
          <button onClick={onComputeTsp} style={{ flex: 1 }}>
            Calculate TSP
          </button>
        </div>
      </div>
    </div>
  );
}
