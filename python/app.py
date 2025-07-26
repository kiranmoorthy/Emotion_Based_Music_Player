import cv2
import time

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'  # Suppress INFO, WARNING, and ERROR logs

import logging
logging.getLogger('tensorflow').setLevel(logging.FATAL)


from deepface import DeepFace
from tensorflow.keras.models import load_model

# Override DeepFace's emotion model loader to load your own weights
def patched_build_model(model_name):
    if model_name == "Emotion":
        return load_model("python/models/emotion_model_weights.h5")
    else:
        return DeepFace.build_model(model_name)

DeepFace.build_model = patched_build_model



def detect_emotion_for_duration_and_save(duration_seconds=1, output_file='../shared/current_emotion.txt'):
    """
    Captures video from the webcam, detects emotions for a specified duration,
    and logs the detected dominant emotion to a text file.

    Args:
        duration_seconds (int): The duration in seconds to run the detection.
        output_file (str): The name of the text file to save the log.
    """
    print("Initializing webcam...")
    # Open the default webcam (camera index 0)
    cap = cv2.VideoCapture(0)

    # Check if the webcam was opened successfully
    if not cap.isOpened():
        print("Error: Could not open webcam. Please check if the camera is connected and not in use by another application.")
        return

    # Start the timer
    start_time = time.time()
    dominant_emotion = "Happy" # Default value if no face is seen

    print(f"Starting emotion detection for {duration_seconds} second(s).")

    # Loop to capture frames for the specified duration
    while (time.time() - start_time) < duration_seconds:
        # Read a single frame from the video feed
        ret, frame = cap.read()

        # If the frame was not captured correctly, break the loop
        if not ret:
            print("Error: Failed to capture frame.")
            break

        # --- Emotion Detection Logic ---
        try:
            # Use DeepFace to analyze the frame for emotion
            # 'enforce_detection=False' prevents crashing if no face is found
            demographies = DeepFace.analyze(
                frame,
                actions=['emotion'],
                enforce_detection=False,
                detector_backend='opencv'
            )

            # Check if any face was detected in the frame
            if len(demographies) > 0:
                # Get the dominant emotion from the first detected face
                dominant_emotion = demographies[0]['dominant_emotion']

                # --- Optional: Display the results on the video feed ---
                # Draw a rectangle and text on the frame for real-time visualization
                region = demographies[0]['region']
                x, y, w, h = region['x'], region['y'], region['w'], region['h']
                cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)
                cv2.putText(
                    frame,
                    f'Emotion: {dominant_emotion}',
                    (x, y - 10),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.9,
                    (0, 255, 0),
                    2
                )

        except Exception as e:
            # This can catch exceptions, for example, if the face detector fails
            # print(f"An error occurred during analysis: {e}")
            pass

        # Display the frame in a window (optional but good for user feedback)
        cv2.imshow('Emotion Detection - Running...', frame)

        # This is a small delay to allow the window to update
        cv2.waitKey(1)

    # --- After the loop (after the duration has passed) ---
    print(f"Detection duration of {duration_seconds} second(s) is complete.")

    # --- Save the detected emotion to the file ---
    #timestamp_str = time.strftime('%Y-%m-%d %H:%M:%S')
    #log_entry = f"Timestamp: {timestamp_str}, Detected Emotion: {dominant_emotion}\n"
    log_entry=dominant_emotion

    # Open the file in write mode to overwrite or create a new file
    with open(output_file, 'w') as f:
        f.write(log_entry)

    print(f"Detected emotion '{dominant_emotion}' saved to '{output_file}'.")

    # --- Cleanup ---
    # Release the webcam resource
    cap.release()
    # Destroy all OpenCV windows
    cv2.destroyAllWindows()
    print("Webcam released. Application finished.")

# --- Main execution block ---
if __name__ == "__main__":
    # You can change the duration (in seconds) here
    detection_duration = 6

    # Ensure cross-platform compatible path
    shared_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'shared'))
    os.makedirs(shared_dir, exist_ok=True)  # Make sure the directory exists

    log_file_name = os.path.join(shared_dir, 'current_emotion.txt')

    detect_emotion_for_duration_and_save(detection_duration, log_file_name)
