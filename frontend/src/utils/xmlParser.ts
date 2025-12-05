import type { MapData, Node, Segment, DeliveryRequest, Warehouse, Delivery, CustomStop } from '../types';

export const parseMapXML = (xmlText: string): MapData => {
  const parser = new DOMParser();
  const xmlDoc = parser.parseFromString(xmlText, "text/xml");

  const nodesMap = new Map<string, Node>();
  const nodes = xmlDoc.getElementsByTagName("noeud");
  
  let minLat = Infinity, maxLat = -Infinity, minLon = Infinity, maxLon = -Infinity;

  for (let i = 0; i < nodes.length; i++) {
    const id = nodes[i].getAttribute("id")!;
    const lat = parseFloat(nodes[i].getAttribute("latitude")!);
    const lon = parseFloat(nodes[i].getAttribute("longitude")!);

    nodesMap.set(id, { id, latitude: lat, longitude: lon });

    if (lat < minLat) minLat = lat;
    if (lat > maxLat) maxLat = lat;
    if (lon < minLon) minLon = lon;
    if (lon > maxLon) maxLon = lon;
  }

  const segments: Segment[] = [];
  const troncons = xmlDoc.getElementsByTagName("troncon");

  for (let i = 0; i < troncons.length; i++) {
    const origin = troncons[i].getAttribute("origine")!;
    const destination = troncons[i].getAttribute("destination")!;
    const length = parseFloat(troncons[i].getAttribute("longueur")!);
    const streetName = troncons[i].getAttribute("nomRue") || "";

    segments.push({ origin, destination, length, streetName });
  }

  // If no nodes, default bounds
  const bounds: [number, number][] = nodesMap.size > 0 
    ? [[minLat, minLon], [maxLat, maxLon]] 
    : [[45.75, 4.85], [45.76, 4.86]];

  return { nodes: nodesMap, segments, bounds };
};

export const parseDeliveryXML = (xmlText: string): DeliveryRequest => {
  const parser = new DOMParser();
  const xmlDoc = parser.parseFromString(xmlText, "text/xml");

  const entrepotEl = xmlDoc.getElementsByTagName("entrepot")[0];
  const warehouse: Warehouse = {
    nodeId: entrepotEl.getAttribute("adresse")!,
    departureTime: entrepotEl.getAttribute("heureDepart")!
  };

  const deliveries: Delivery[] = [];
  const livraisons = xmlDoc.getElementsByTagName("livraison");

  for (let i = 0; i < livraisons.length; i++) {
    const pickupNodeId = livraisons[i].getAttribute("adresseEnlevement")!;
    const deliveryNodeId = livraisons[i].getAttribute("adresseLivraison")!;
    const pickupDuration = parseInt(livraisons[i].getAttribute("dureeEnlevement")!);
    const deliveryDuration = parseInt(livraisons[i].getAttribute("dureeLivraison")!);

    deliveries.push({
      id: `delivery-${i}`,
      pickupNodeId,
      deliveryNodeId,
      pickupDuration,
      deliveryDuration
    });
  }

  return { warehouse, deliveries };
};

export const findNearestNode = (mapData: MapData, lat: number, lng: number): Node | null => {
  let nearestNode: Node | null = null;
  let minDistance = Infinity;

  mapData.nodes.forEach((node) => {
    // Simple Euclidean distance approximation for performance
    // For more accuracy, use Haversine formula, but for map clicking this is usually sufficient
    // considering the scale of a city.
    const dLat = node.latitude - lat;
    const dLng = node.longitude - lng;
    const distance = dLat * dLat + dLng * dLng;

    if (distance < minDistance) {
      minDistance = distance;
      nearestNode = node;
    }
  });

  return nearestNode;
};

export const generateDeliveryXML = (deliveryRequest: DeliveryRequest | null, customStops: CustomStop[]): string => {
  if (!deliveryRequest && customStops.length === 0) {
    return "";
  }

  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n<demandeDeLivraisons>\n';

  // Warehouse
  if (deliveryRequest?.warehouse) {
    xml += `    <entrepot adresse="${deliveryRequest.warehouse.nodeId}" heureDepart="${deliveryRequest.warehouse.departureTime}"/>\n`;
  } else {
    // Default warehouse if not present but custom stops exist? 
    // Ideally we should have a warehouse set if we are saving.
    // For now, let's assume if deliveryRequest is null, we might not have a warehouse, 
    // but the user might have set one via "Set Warehouse".
    // However, the current App state updates deliveryRequest when setting warehouse.
    // So deliveryRequest should be populated if a warehouse is set.
  }

  // Existing Deliveries
  if (deliveryRequest?.deliveries) {
    deliveryRequest.deliveries.forEach(d => {
      xml += `    <livraison adresseEnlevement="${d.pickupNodeId}" adresseLivraison="${d.deliveryNodeId}" dureeEnlevement="${d.pickupDuration}" dureeLivraison="${d.deliveryDuration}"/>\n`;
    });
  }

  // Custom Deliveries
  // customStops are stored as [pickup, delivery, pickup, delivery, ...]
  for (let i = 0; i < customStops.length; i += 2) {
    const pickup = customStops[i];
    const delivery = customStops[i + 1];
    if (pickup && delivery) {
      xml += `    <livraison adresseEnlevement="${pickup.nodeId}" adresseLivraison="${delivery.nodeId}" dureeEnlevement="${pickup.duration || 0}" dureeLivraison="${delivery.duration || 0}"/>\n`;
    }
  }

  xml += '</demandeDeLivraisons>';
  return xml;
};
