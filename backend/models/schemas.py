from pydantic import BaseModel
from typing import List, Optional

class DetectedObject(BaseModel):
    name: str
    confidence: float
    x_center: float
    y_center: float
    width: float
    height: float
    spatial_zone: str

class Place(BaseModel):
    id: str
    name: str
    user_id: str
    objects: List[DetectedObject]
    scene_description: str
    image_path: str

class ChangeResult(BaseModel):
    new_objects: List[DetectedObject]
    removed_objects: List[DetectedObject]
    moved_objects: List[DetectedObject]
    changes_summary: str
    alert_messages: List[str]
    has_important_changes: bool
    tts_message: str

class SavePlaceResponse(BaseModel):
    place_id: str
    message: str
    scene_description: str

class CheckChangesResponse(BaseModel):
    change_result: ChangeResult
    message: str
