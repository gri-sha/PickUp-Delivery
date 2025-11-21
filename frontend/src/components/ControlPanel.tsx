import React from 'react';
import type { StopType } from '../types';

interface ControlPanelProps {
  onLoadMap: (file: File) => void;
  onLoadDelivery: (file: File) => void;
  courierCount: number;
  setCourierCount: (count: number) => void;
  onAddStop: (type: StopType) => void;
  onLocateUser: () => void;
  onSaveRequest: () => void;
  onSendRequest: () => void;
}

export default function ControlPanel({
  onLoadMap,
  onLoadDelivery,
  courierCount,
  setCourierCount,
  onAddStop,
  onLocateUser,
  onSaveRequest,
  onSendRequest
}: ControlPanelProps) {
  const [selectedStopType, setSelectedStopType] = React.useState<StopType>('pickup');
  
  const handleMapUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      onLoadMap(e.target.files[0]);
    }
  };

  const handleDeliveryUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      onLoadDelivery(e.target.files[0]);
    }
  };

  return (
    <div className="control-panel" style={{ padding: '20px', borderTop: '1px solid #ccc' }}>
      <div style={{ marginBottom: '10px' }}>
        <button onClick={onLocateUser}>Locate Me</button>
      </div>

      <div style={{ marginBottom: '10px' }}>
        <label>Load Map XML: </label>
        <input type="file" accept=".xml" onChange={handleMapUpload} />
      </div>

      <div style={{ marginBottom: '10px' }}>
        <label>Load Delivery Request XML: </label>
        <input type="file" accept=".xml" onChange={handleDeliveryUpload} />
      </div>

      <div style={{ marginBottom: '10px' }}>
        <label>Number of Couriers: </label>
        <input 
          type="number" 
          min="1" 
          value={courierCount} 
          onChange={(e) => setCourierCount(parseInt(e.target.value))} 
        />
      </div>

      <div style={{ marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '10px' }}>
        <select 
          value={selectedStopType} 
          onChange={(e) => setSelectedStopType(e.target.value as StopType)}
          style={{ padding: '5px' }}
        >
          <option value="pickup">Pickup</option>
          <option value="delivery">Delivery</option>
          <option value="warehouse">Warehouse</option>
        </select>
        <button onClick={() => onAddStop(selectedStopType)}>Add Stop</button>
      </div>

      <div style={{ marginBottom: '10px' }}>
        <button onClick={onSaveRequest} style={{ marginRight: '10px' }}>Save Request</button>
        <button onClick={onSendRequest}>Send to Server</button>
      </div>
    </div>
  );
}
