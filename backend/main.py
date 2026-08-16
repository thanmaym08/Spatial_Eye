from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from typing import Optional, List
import os
import uuid
import json

from config import config
from models.schemas import SavePlaceResponse, CheckChangesResponse, ChangeResult, DetectedObject
from detection.yolo_detector import detect_objects
from detection.gemini_analyzer import analyze_scene, describe_changes
from memory.spatial_memory import save_place, get_place, get_all_places, delete_place, log_change, check_db_connection
from comparison.change_detector import compare_environments

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Warm up YOLO model
    print(f"Loading YOLO model {config.YOLO_MODEL_NAME}...")
    db_status = "connected" if check_db_connection() else "disconnected/unreachable"
    print(f"MongoDB status at {config.MONGODB_URI}: {db_status}")
    yield

app = FastAPI(title="Facial AI Backend", lifespan=lifespan)

@app.get("/api/health")
async def api_health():
    db_connected = check_db_connection()
    return {
        "status": "healthy",
        "mongodb": {
            "uri": config.MONGODB_URI,
            "database": config.DATABASE_NAME,
            "connected": db_connected
        }
    }


# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

IMAGES_DIR = "images"
os.makedirs(IMAGES_DIR, exist_ok=True)

@app.post("/api/save-place", response_model=SavePlaceResponse)
async def api_save_place(
    file: UploadFile = File(...),
    name: str = Form(...),
    user_id: str = Form("default")
):
    try:
        contents = await file.read()
        
        # Save image
        ext = file.filename.split(".")[-1] if "." in file.filename else "jpg"
        filename = f"{uuid.uuid4()}.{ext}"
        filepath = os.path.join(IMAGES_DIR, filename)
        with open(filepath, "wb") as f:
            f.write(contents)
            
        # Detect objects
        objects = detect_objects(contents)
        
        # Analyze scene
        scene_description = analyze_scene(contents)
        
        # Save to DB
        place_id = save_place(name, user_id, objects, scene_description, filepath)
        
        return SavePlaceResponse(
            place_id=place_id,
            message="Place saved successfully.",
            scene_description=scene_description
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/check-changes", response_model=CheckChangesResponse)
async def api_check_changes(
    file: UploadFile = File(...),
    place_id: str = Form(...)
):
    try:
        place_dict = get_place(place_id)
        if not place_dict:
            raise HTTPException(status_code=404, detail="Place not found.")
            
        contents = await file.read()
        
        # Detect objects
        new_objects = detect_objects(contents)
        
        # Reconstruct old objects from dict
        old_objects = [DetectedObject(**obj) for obj in place_dict["objects"]]
        
        # Compare
        change_result = compare_environments(old_objects, new_objects)
        
        if change_result.has_important_changes or change_result.changes_summary != "No significant changes.":
            new_scene_desc = analyze_scene(contents)
            tts_msg = describe_changes(place_dict["scene_description"], new_scene_desc, change_result.changes_summary)
            change_result.tts_message = tts_msg
            
            # Log changes
            for obj in change_result.new_objects:
                log_change(place_id, "NEW", obj.name, f"Appeared in {obj.spatial_zone}", "UNKNOWN", 0)
            for obj in change_result.moved_objects:
                log_change(place_id, "MOVED", obj.name, f"Moved to {obj.spatial_zone}", "UNKNOWN", 0)
        else:
            change_result.tts_message = "No significant changes detected."
            
        return CheckChangesResponse(
            change_result=change_result,
            message="Changes checked successfully."
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/places")
async def api_get_places(user_id: str = "default"):
    return get_all_places(user_id)

@app.get("/api/place/{place_id}")
async def api_get_place(place_id: str):
    place = get_place(place_id)
    if not place:
        raise HTTPException(status_code=404, detail="Place not found")
    return place

@app.delete("/api/place/{place_id}")
async def api_delete_place(place_id: str):
    success = delete_place(place_id)
    if not success:
        raise HTTPException(status_code=404, detail="Place not found or could not be deleted")
    return {"message": "Place deleted successfully."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
