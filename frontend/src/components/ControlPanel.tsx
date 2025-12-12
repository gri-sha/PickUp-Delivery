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
  onStartCollectNodes: () => void;
  onStopCollectNodes: () => void;
  onSaveClickedNodes: () => void;
  onClearClickedNodes: () => void;
  isCollectingNodes: boolean;
  clickedNodesCount: number;
  onComputeTsp: (planName: string, requestName: string) => void;
  onUploadAndComputeTsp: (planFile: File, requestFile: File) => void;
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
  onStartCollectNodes,
  onStopCollectNodes,
  onSaveClickedNodes,
  onClearClickedNodes,
  isCollectingNodes,
  clickedNodesCount,
  onComputeTsp,
  onUploadAndComputeTsp,
}: ControlPanelProps) {
  const [MAP_FILES, setMapFiles] = useState<string[]>([]);
  const [REQUEST_FILES, setRequestFiles] = useState<string[]>([]);

  const [selectedMap, setSelectedMap] = useState<string>("");
  const [selectedRequest, setSelectedRequest] = useState<string>("");

  const [uploadPlanFile, setUploadPlanFile] = useState<File | null>(null);
  const [uploadRequestFile, setUploadRequestFile] = useState<File | null>(null);


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

        {/* Upload XML Files Section */}
        <div
          style={{
            border: "2px solid #1f2937",
            padding: "8px",
            marginTop: "10px",
            backgroundColor: "#f0f9ff",
          }}
        >
          <h4 style={{ marginTop: 0, fontSize: "0.9rem" }}>Upload & Compute TSP</h4>
          <p style={{ fontSize: "0.75rem", color: "#6b7280", marginBottom: "8px" }}>
            Upload your own XML files to compute TSP
          </p>
          <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
            <div>
              <label style={{ fontSize: "0.8rem", display: "block", marginBottom: "4px" }}>
                Plan XML:
              </label>
              <input
                type="file"
                accept=".xml"
                onChange={(e) => setUploadPlanFile(e.target.files?.[0] || null)}
                style={{ fontSize: "0.8rem", width: "100%" }}
              />
              {uploadPlanFile && (
                <div style={{ fontSize: "0.7rem", color: "#059669", marginTop: "2px" }}>
                  ✓ {uploadPlanFile.name}
                </div>
              )}
            </div>
            <div>
              <label style={{ fontSize: "0.8rem", display: "block", marginBottom: "4px" }}>
                Request XML:
              </label>
              <input
                type="file"
                accept=".xml"
                onChange={(e) => setUploadRequestFile(e.target.files?.[0] || null)}
                style={{ fontSize: "0.8rem", width: "100%" }}
              />
              {uploadRequestFile && (
                <div style={{ fontSize: "0.7rem", color: "#059669", marginTop: "2px" }}>
                  ✓ {uploadRequestFile.name}
                </div>
              )}
            </div>
            <button
              onClick={() => {
                if (uploadPlanFile && uploadRequestFile) {
                  onUploadAndComputeTsp(uploadPlanFile, uploadRequestFile);
                }
              }}
              disabled={!uploadPlanFile || !uploadRequestFile}
              style={{
                backgroundColor: uploadPlanFile && uploadRequestFile ? "#3b82f6" : "#e5e7eb",
                color: uploadPlanFile && uploadRequestFile ? "#ffffff" : "#9ca3af",
                cursor: uploadPlanFile && uploadRequestFile ? "pointer" : "not-allowed",
                padding: "8px",
                fontWeight: "bold",
              }}
            >
              Upload & Calculate TSP
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
          <button
            onClick={() => onComputeTsp(selectedMap, selectedRequest)}
            style={{ flex: 1 }}
            disabled={!selectedMap || !selectedRequest}
          >
            Calculate TSP
          </button>
        </div>
      </div>
    </div>
  );
}
