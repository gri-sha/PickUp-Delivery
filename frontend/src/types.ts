export interface Node {
  id: string;
  latitude: number;
  longitude: number;
}

export interface Segment {
  origin: string;
  destination: string;
  length: number;
  streetName: string;
}

export interface MapData {
  nodes: Map<string, Node>;
  segments: Segment[];
  bounds: [number, number][]; // [minLat, minLon], [maxLat, maxLon]
}

export interface Delivery {
  id: string; // Generated ID
  pickupNodeId: string;
  deliveryNodeId: string;
  pickupDuration: number;
  deliveryDuration: number;
}

export interface Warehouse {
  nodeId: string;
  departureTime: string;
}

export interface DeliveryRequest {
  warehouse: Warehouse;
  deliveries: Delivery[];
}

export type StopType = 'pickup' | 'delivery' | 'warehouse';

export interface CustomStop {
  nodeId: string;
  latitude: number;
  longitude: number;
  type: StopType;
  duration?: number;
}

export interface TspPath {
  nodeIds: string[];
}

