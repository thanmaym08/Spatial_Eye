import os

class Config:
    MONGODB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")
    DATABASE_NAME = "facial_ai_db"
    GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
    YOLO_MODEL_NAME = "yolov8n.pt"
    ALERT_THRESHOLD = 6
    MOVEMENT_THRESHOLD = 0.15

config = Config()
