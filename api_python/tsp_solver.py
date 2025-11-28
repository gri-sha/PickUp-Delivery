import networkx as nx
from models import *
from utils import load_plan_graph, get_shortest_path


def solve_tsp(request: DeliveryRequest) -> list[CourierRoute]:
    G = load_plan_graph(request.plan_file)

    warehouse_id = request.warehouse_node_id
    couriers_num = request.couriers_number
    points = request.points

    # 1. Identify all key nodes (Warehouse + Pickups + Deliveries)
    key_nodes = {warehouse_id}
    for p in points:
        key_nodes.add(p.pickup_node_id)
        key_nodes.add(p.delivery_node_id)

    # 2. Compute Distance Matrix
    # We need distances between all pairs of key nodes.
    # Using single_source_dijkstra for each key node is efficient.
    dist_matrix = {}
    path_cache = {}  # Store the actual paths to avoid re-computing later

    for start_node in key_nodes:
        try:
            lengths, paths = nx.single_source_dijkstra(G, start_node, weight="weight")
            dist_matrix[start_node] = lengths
            path_cache[start_node] = paths
        except nx.NetworkXError:
            # Handle case where node is not in graph or unreachable
            pass

    # Helper to get distance
    def get_dist(u, v):
        if u not in dist_matrix or v not in dist_matrix[u]:
            return float("inf")
        return dist_matrix[u][v]

    # 3. Greedy Insertion
    # Initialize routes: each is just [Warehouse, Warehouse]
    # We represent a route as a list of node IDs.
    routes = [[warehouse_id, warehouse_id] for _ in range(couriers_num)]

    unassigned = points[:]

    while unassigned:
        best_cost = float("inf")
        best_request = None
        best_courier_idx = -1
        best_insertion = None  # (pickup_index, delivery_index)

        for req in unassigned:
            pickup = req.pickup_node_id
            delivery = req.delivery_node_id

            for i in range(couriers_num):
                route = routes[i]
                # Try all valid insertion positions
                # Pickup at index j (1 <= j < len(route)) - insert before node at j
                # Delivery at index k (j < k <= len(route)) - insert before node at k (which is now at k+1 after pickup insertion)

                # Current route cost
                current_route_cost = 0
                for idx in range(len(route) - 1):
                    current_route_cost += get_dist(route[idx], route[idx + 1])

                # Iterate over all possible positions
                # Route has N nodes (N-1 edges).
                # We can insert pickup at any position from 1 to N-1 (between 0-1, 1-2, ..., N-2-N-1)
                # Actually we can insert between any two nodes.
                # Route: [W, A, B, W]
                # Insert P: [W, P, A, B, W], [W, A, P, B, W], [W, A, B, P, W]

                for p_idx in range(1, len(route)):  # Insert P at p_idx
                    for d_idx in range(
                        p_idx + 1, len(route) + 2
                    ):  # Insert D at d_idx (accounting for P being added)

                        # Construct temp route to calculate cost
                        # Original: route[:p_idx] + [P] + route[p_idx:d_idx-1] + [D] + route[d_idx-1:]
                        # Wait, indices are tricky.

                        # Let's simplify:
                        # New route would be:
                        # route[0...p_idx-1] -> P -> route[p_idx...d_idx-2] -> D -> route[d_idx-2...]
                        # Note: d_idx is index in the NEW route.

                        # Let's just simulate the list
                        temp_route = route[:]
                        temp_route.insert(p_idx, pickup)
                        temp_route.insert(d_idx, delivery)

                        # Calculate new cost
                        new_cost = 0
                        possible = True
                        for idx in range(len(temp_route) - 1):
                            d = get_dist(temp_route[idx], temp_route[idx + 1])
                            if d == float("inf"):
                                possible = False
                                break
                            new_cost += d

                        if possible:
                            added_cost = new_cost - current_route_cost
                            if added_cost < best_cost:
                                best_cost = added_cost
                                best_request = req
                                best_courier_idx = i
                                best_insertion = (p_idx, d_idx)

        if best_request:
            # Apply best insertion
            c_idx = best_courier_idx
            p_idx, d_idx = best_insertion
            routes[c_idx].insert(p_idx, best_request.pickup_node_id)
            routes[c_idx].insert(d_idx, best_request.delivery_node_id)
            unassigned.remove(best_request)
        else:
            # Cannot assign remaining requests (unreachable or graph disconnected)
            raise ValueError(
                "Cannot assign all requests. Some points might be unreachable."
            )

    # 4. Construct detailed routes (RouteSegments)
    final_routes = []
    for i, route_nodes in enumerate(routes):
        segments = []
        total_len = 0.0

        for j in range(len(route_nodes) - 1):
            u = route_nodes[j]
            v = route_nodes[j + 1]

            # Get the full path of nodes between u and v
            # path_cache[u][v] gives list of nodes [u, ..., v]
            if u in path_cache and v in path_cache[u]:
                node_path = path_cache[u][v]

                # Convert node path to segments (edges)
                for k in range(len(node_path) - 1):
                    n1 = node_path[k]
                    n2 = node_path[k + 1]
                    edge_data = G.get_edge_data(n1, n2)

                    length = edge_data["weight"]
                    name = edge_data.get("name", "")

                    segments.append(
                        RouteSegment(
                            origin_id=n1,
                            destination_id=n2,
                            length=length,
                            street_name=name,
                        )
                    )
                    total_len += length
            else:
                # Should not happen if get_dist returned a value
                pass

        final_routes.append(
            CourierRoute(courier_id=i + 1, route=segments, total_length=total_len)
        )

    return final_routes
