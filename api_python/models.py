from pydantic import BaseModel

class DeliveryPoint(BaseModel):
    pickup_node_id: str
    delivery_node_id: str
    pickup_duration: int = 0
    delivery_duration: int = 0


class DeliveryRequest(BaseModel):
    warehouse_node_id: str
    couriers_number: int
    points: list[DeliveryPoint]
    plan_file: str = "grandPlan.xml"


class RouteSegment(BaseModel):
    origin_id: str
    destination_id: str
    length: float
    street_name: str


class CourierRoute(BaseModel):
    courier_id: int
    route: list[RouteSegment]
    total_length: float
