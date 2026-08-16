# Setup Instructions for Facial AI Android App

Welcome to the Facial AI Android App source code. Follow these steps to load and run the app in Android Studio.

## Prerequisites
1. Install [Android Studio](https://developer.android.com/studio) (latest version recommended).
2. Ensure you have the Android SDK for API 34 installed.

## Getting Started

1. **Open Android Studio**.
2. If you haven't created a project yet, you can create one with the Empty Views Activity template (Name: "Facial AI", Package: `com.facialai`, Language: Kotlin, Minimum API: 26). Then copy this entire `app` directory over the generated one.
3. Alternatively, simply select **Open** from the Android Studio welcome screen, and point it to the directory containing this `build.gradle.kts` file (the root of the project).
4. Wait for Gradle to sync dependencies.

## Testing on Physical Devices vs. Emulator

By default, the `RetrofitClient.kt` uses the URL `http://10.0.2.2:8000` which points to `localhost:8000` on your development machine (from an emulator).

**If you are testing on a physical Android device:**
1. Connect your Android device to the same Wi-Fi network as your development computer.
2. Find your computer's local IP address (e.g., `192.168.1.5`).
3. Open `app/src/main/java/com/facialai/api/RetrofitClient.kt`.
4. Change `private const val BASE_URL = "http://10.0.2.2:8000"` to `private const val BASE_URL = "http://YOUR_LOCAL_IP:8000"`.
5. Ensure your backend server is running and accessible on that port (e.g. `uvicorn main:app --host 0.0.0.0 --port 8000`).

## Permissions
The app uses the following permissions:
- **Camera:** Needed for capturing place images.
- **Internet:** Needed to communicate with the backend API.
- **Vibrate:** Needed for haptic feedback when risk is detected.

Ensure you grant these permissions when running the app for the first time.
