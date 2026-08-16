from google import genai
from config import config
from PIL import Image
import io

def get_client():
    if config.GEMINI_API_KEY:
        return genai.Client(api_key=config.GEMINI_API_KEY)
    return None

def analyze_scene(image_bytes: bytes) -> str:
    """Returns a general scene description from the image."""
    client = get_client()
    if not client:
        return "Scene description unavailable due to missing Gemini API key."
    
    try:
        image = Image.open(io.BytesIO(image_bytes))
        prompt = "Describe the scene in this image concisely, focusing on layout and objects. Keep it under 3 sentences."
        
        response = client.models.generate_content(
            model='gemini-2.0-flash',
            contents=[prompt, image],
            config=genai.types.GenerateContentConfig(
                temperature=0.3
            )
        )
        return response.text
    except Exception as e:
        return f"Error analyzing scene: {str(e)}"

def describe_changes(old_description: str, new_description: str, changes_summary: str) -> str:
    """Returns a natural language change description for TTS."""
    client = get_client()
    if not client:
        return f"Changes detected: {changes_summary}"
    
    try:
        prompt = f"""
        You are an assistant for a visually impaired user. Summarize the following changes in the environment clearly and concisely for Text-to-Speech (TTS).
        Previous scene: {old_description}
        New scene: {new_description}
        Detected object changes: {changes_summary}
        
        Provide a short, direct message stating what has changed, disappeared, or moved. Focus on safety and spatial awareness.
        """
        response = client.models.generate_content(
            model='gemini-2.0-flash',
            contents=prompt,
            config=genai.types.GenerateContentConfig(
                temperature=0.3
            )
        )
        return response.text.strip()
    except Exception as e:
        return f"Changes: {changes_summary}"
