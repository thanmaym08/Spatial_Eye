from typing import List, Dict
import numpy as np
from scipy.optimize import linear_sum_assignment
from models.schemas import DetectedObject, ChangeResult
from config import config

# Danger scores for COCO classes (0-10)
# Higher is more dangerous for a visually impaired person
DANGER_SCORES = {
    'person': 2, 'bicycle': 7, 'car': 9, 'motorcycle': 8, 'airplane': 0, 
    'bus': 8, 'train': 8, 'truck': 9, 'boat': 0, 'traffic light': 0, 
    'fire hydrant': 5, 'stop sign': 0, 'parking meter': 3, 'bench': 5, 
    'bird': 1, 'cat': 2, 'dog': 3, 'horse': 4, 'sheep': 2, 'cow': 4, 
    'elephant': 0, 'bear': 0, 'zebra': 0, 'giraffe': 0, 'backpack': 4, 
    'umbrella': 3, 'handbag': 2, 'tie': 0, 'suitcase': 6, 'frisbee': 1, 
    'skis': 4, 'snowboard': 4, 'sports ball': 3, 'kite': 1, 'baseball bat': 3, 
    'baseball glove': 1, 'skateboard': 6, 'surfboard': 0, 'tennis racket': 1, 
    'bottle': 4, 'wine glass': 7, 'cup': 4, 'fork': 3, 'knife': 8, 
    'spoon': 2, 'bowl': 3, 'banana': 3, 'apple': 2, 'sandwich': 1, 
    'orange': 2, 'broccoli': 1, 'carrot': 1, 'hot dog': 1, 'pizza': 1, 
    'donut': 1, 'cake': 1, 'chair': 6, 'couch': 5, 'potted plant': 4, 
    'bed': 4, 'dining table': 6, 'toilet': 3, 'tv': 2, 'laptop': 2, 
    'mouse': 1, 'remote': 1, 'keyboard': 1, 'cell phone': 1, 'microwave': 2, 
    'oven': 3, 'toaster': 3, 'sink': 2, 'refrigerator': 4, 'book': 2, 
    'clock': 1, 'vase': 6, 'scissors': 8, 'teddy bear': 1, 'hair drier': 3, 
    'toothbrush': 1
}

def get_danger_score(class_name: str) -> int:
    return DANGER_SCORES.get(class_name.lower(), 3)

def compute_distance(obj1: DetectedObject, obj2: DetectedObject) -> float:
    """Compute Euclidean distance between normalized centers."""
    return float(np.sqrt((obj1.x_center - obj2.x_center)**2 + (obj1.y_center - obj2.y_center)**2))

def compare_environments(old_objects: List[DetectedObject], new_objects: List[DetectedObject]) -> ChangeResult:
    """Compare old and new object lists using Hungarian matching."""
    
    # Group objects by class name
    old_by_class: Dict[str, List[DetectedObject]] = {}
    for obj in old_objects:
        old_by_class.setdefault(obj.name, []).append(obj)
        
    new_by_class: Dict[str, List[DetectedObject]] = {}
    for obj in new_objects:
        new_by_class.setdefault(obj.name, []).append(obj)
        
    all_classes = set(old_by_class.keys()).union(new_by_class.keys())
    
    new_list = []
    removed_list = []
    moved_list = []
    alert_messages = []
    changes_summary_parts = []
    
    max_danger_score = 0
    
    for cls in all_classes:
        old_objs = old_by_class.get(cls, [])
        new_objs = new_by_class.get(cls, [])
        
        n_old = len(old_objs)
        n_new = len(new_objs)
        
        if n_old == 0 and n_new > 0:
            new_list.extend(new_objs)
            danger = get_danger_score(cls)
            max_danger_score = max(max_danger_score, danger)
            if danger >= config.ALERT_THRESHOLD:
                alert_messages.append(f"Caution: New {cls} detected in the {new_objs[0].spatial_zone}.")
            changes_summary_parts.append(f"New {cls} appeared.")
            continue
            
        if n_new == 0 and n_old > 0:
            removed_list.extend(old_objs)
            changes_summary_parts.append(f"{cls} is missing.")
            continue
            
        # Hungarian matching for same class
        cost_matrix = np.zeros((n_old, n_new))
        for i, o in enumerate(old_objs):
            for j, n in enumerate(new_objs):
                cost_matrix[i, j] = compute_distance(o, n)
                
        row_ind, col_ind = linear_sum_assignment(cost_matrix)
        
        # Track matched
        matched_old = set()
        matched_new = set()
        
        for r, c in zip(row_ind, col_ind):
            dist = cost_matrix[r, c]
            matched_old.add(r)
            matched_new.add(c)
            
            if dist > config.MOVEMENT_THRESHOLD:
                moved_list.append(new_objs[c])
                danger = get_danger_score(cls)
                max_danger_score = max(max_danger_score, danger)
                if danger >= config.ALERT_THRESHOLD:
                    alert_messages.append(f"Caution: {cls} moved to the {new_objs[c].spatial_zone}.")
                changes_summary_parts.append(f"{cls} moved from {old_objs[r].spatial_zone} to {new_objs[c].spatial_zone}.")
                
        # Unmatched old
        for i, o in enumerate(old_objs):
            if i not in matched_old:
                removed_list.append(o)
                changes_summary_parts.append(f"{cls} is missing.")
                
        # Unmatched new
        for j, n in enumerate(new_objs):
            if j not in matched_new:
                new_list.append(n)
                danger = get_danger_score(cls)
                max_danger_score = max(max_danger_score, danger)
                if danger >= config.ALERT_THRESHOLD:
                    alert_messages.append(f"Caution: Additional {cls} detected in the {n.spatial_zone}.")
                changes_summary_parts.append(f"New {cls} appeared.")

    changes_summary = " ".join(changes_summary_parts) if changes_summary_parts else "No significant changes."
    has_important_changes = max_danger_score >= config.ALERT_THRESHOLD or len(new_list) > 0 or len(moved_list) > 0
    
    return ChangeResult(
        new_objects=new_list,
        removed_objects=removed_list,
        moved_objects=moved_list,
        changes_summary=changes_summary,
        alert_messages=alert_messages,
        has_important_changes=has_important_changes,
        tts_message=""
    )
