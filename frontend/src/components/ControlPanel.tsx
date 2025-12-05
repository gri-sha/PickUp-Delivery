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
  onSendRequest: () => void;
  deliveryCreationStep: "idle" | "select-pickup" | "select-delivery" | "review";
  pickupDuration: number | string;
  setPickupDuration: (duration: number | string) => void;
  deliveryDuration: number | string;
  setDeliveryDuration: (duration: number | string) => void;
  onSetWarehouse: () => void;
  isSettingWarehouse: boolean;
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
  onSendRequest,
  deliveryCreationStep,
  pickupDuration,
  setPickupDuration,
  deliveryDuration,
  setDeliveryDuration,
  onSetWarehouse,
  isSettingWarehouse,
}: ControlPanelProps) {
  const [MAP_FILES, setMapFiles] = useState<string[]>([]);
  const [REQUEST_FILES, setRequestFiles] = useState<string[]>([]);

  const [selectedMap, setSelectedMap] = useState<string>("");
  const [selectedRequest, setSelectedRequest] = useState<string>("");

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
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "5px" }}>
            <label>Select Map:</label>
            <button onClick={fetchMapFiles} style={{ padding: "4px 8px", fontSize: "0.8rem", display: "flex", alignItems: "center", gap: "4px", cursor: "pointer" }} title="Refresh Map List">
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
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "5px" }}>
            <label>Select Delivery Request:</label>
            <button onClick={fetchRequestFiles} style={{ padding: "4px 8px", fontSize: "0.8rem", display: "flex", alignItems: "center", gap: "4px", cursor: "pointer" }} title="Refresh Request List">
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
                  : deliveryCreationStep === "review" ? "#86efac" : "#ffffff",
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
          <button onClick={onSendRequest} style={{ flex: 1 }}>
            Calculate TSP
          </button>
        </div>
      </div>
    </div>
  );
}
