import io
from PIL import Image
import numpy as np
from ultralytics import YOLO
from models.schemas import DetectedObject
from config import config

# Load model globally to avoid reloading
model = YOLO(config.YOLO_MODEL_NAME)

def get_spatial_zone(x_center: float, y_center: float) -> str:
    """Determine spatial zone based on 3x3 grid."""
    if y_center < 0.33:
        y_zone = "top"
    elif y_center < 0.66:
        y_zone = "center"
    else:
        y_zone = "bottom"
        
    if x_center < 0.33:
        x_zone = "left"
    elif x_center < 0.66:
        x_zone = "center"
    else:
        x_zone = "right"
        
    if y_zone == "center" and x_zone == "center":
        return "center"
    return f"{y_zone}-{x_zone}"

def detect_objects(image_bytes: bytes) -> list[DetectedObject]:
    """Detect objects in an image using YOLOv8."""
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    results = model(image, conf=0.30)[0]
    
    detected_objects = []
    
    # Process results
    for box in results.boxes:
        cls_id = int(box.cls[0].item())
        class_name = results.names[cls_id]
        confidence = float(box.conf[0].item())
        
        # Get normalized coordinates
        x_center, y_center, width, height = box.xywhn[0].tolist()
        
        spatial_zone = get_spatial_zone(x_center, y_center)
        
        obj = DetectedObject(
            name=class_name,
            confidence=confidence,
            x_center=x_center,
            y_center=y_center,
            width=width,
            height=height,
            spatial_zone=spatial_zone
        )
        detected_objects.append(obj)
        
    return detected_objects
