import os
import xml.etree.ElementTree as ET
import networkx as nx
from typing import List, Tuple

XML_FOLDER = "xml"
PLANS_FOLDER = os.path.join(XML_FOLDER, "plans")
REQUESTS_FOLDER = os.path.join(XML_FOLDER, "requests")

# Cache for loaded graphs
_graph_cache = {}


def get_xml_files(folder: str) -> List[str]:
    """Returns a list of XML filenames in the given folder."""
    if not os.path.exists(folder):
        return []
    return [f for f in os.listdir(folder) if f.endswith(".xml")]


def load_plan_graph(plan_filename: str) -> nx.DiGraph:
    """Parses a plan XML file and returns a NetworkX DiGraph."""
    if plan_filename in _graph_cache:
        return _graph_cache[plan_filename]

    file_path = os.path.join(PLANS_FOLDER, plan_filename)
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Plan file {plan_filename} not found.")

    tree = ET.parse(file_path)
    root = tree.getroot()

    G = nx.DiGraph()

    # Parse nodes
    for noeud in root.findall("noeud"):
        node_id = noeud.get("id")
        lat = float(noeud.get("latitude"))
        lon = float(noeud.get("longitude"))
        G.add_node(node_id, pos=(lat, lon))

    # Parse edges (troncons)
    for troncon in root.findall("troncon"):
        origin = troncon.get("origine")
        destination = troncon.get("destination")
        length = float(troncon.get("longueur"))
        street_name = troncon.get("nomRue")

        # Add edge with attributes
        G.add_edge(origin, destination, weight=length, name=street_name)

    _graph_cache[plan_filename] = G
    return G


def get_shortest_path(
    G: nx.DiGraph, source: str, target: str
) -> Tuple[float, List[str]]:
    """Returns the shortest path length and the list of nodes."""
    try:
        length = nx.shortest_path_length(
            G, source=source, target=target, weight="weight"
        )
        path = nx.shortest_path(G, source=source, target=target, weight="weight")
        return length, path
    except nx.NetworkXNoPath:
        return float("inf"), []
