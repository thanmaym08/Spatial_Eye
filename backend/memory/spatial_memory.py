from pymongo import MongoClient
from bson.objectid import ObjectId
from typing import List, Dict, Any, Optional
from config import config
from models.schemas import DetectedObject, Place

# Initialize MongoDB connection
client = MongoClient(config.MONGODB_URI)
db = client[config.DATABASE_NAME]
places_collection = db['places']
logs_collection = db['logs']

def save_place(name: str, user_id: str, objects: List[DetectedObject], scene_description: str, image_path: str) -> str:
    """Saves a new place to MongoDB."""
    place_doc = {
        "name": name,
        "user_id": user_id,
        "objects": [obj.model_dump() for obj in objects],
        "scene_description": scene_description,
        "image_path": image_path
    }
    result = places_collection.insert_one(place_doc)
    return str(result.inserted_id)

def get_place(place_id: str) -> Optional[Dict[str, Any]]:
    """Retrieves a place by ID."""
    try:
        place = places_collection.find_one({"_id": ObjectId(place_id)})
        if place:
            place['_id'] = str(place['_id'])
        return place
    except Exception:
        return None

def get_all_places(user_id: str) -> List[Dict[str, Any]]:
    """Retrieves all places for a user."""
    places = list(places_collection.find({"user_id": user_id}))
    for place in places:
        place['_id'] = str(place['_id'])
    return places

def delete_place(place_id: str) -> bool:
    """Deletes a place by ID."""
    try:
        result = places_collection.delete_one({"_id": ObjectId(place_id)})
        return result.deleted_count > 0
    except Exception:
        return False

def log_change(place_id: str, change_type: str, object_name: str, description: str, risk_level: str, danger_score: int):
    """Logs an detected change for auditing and history."""
    log_doc = {
        "place_id": place_id,
        "change_type": change_type,
        "object_name": object_name,
        "description": description,
        "risk_level": risk_level,
        "danger_score": danger_score
    }
    logs_collection.insert_one(log_doc)
