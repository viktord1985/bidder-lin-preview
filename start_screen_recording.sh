#!/bin/bash

# === SETTINGS ===
FOLDER="export/video_logs"
MAX_FOLDER_SIZE_MB=5000
FILENAME="$1_recording_$(date +"%Y-%m-%d_%H-%M-%S").mp4"
FULL_PATH="$FOLDER/$FILENAME"

mkdir -p "$FOLDER"

# === CLEANUP IF THE FOLDER IS FULL ===
folder_size_mb=$(du -sm "$FOLDER" | cut -f1)
while [ "$folder_size_mb" -gt "$MAX_FOLDER_SIZE_MB" ]; do
  oldest_file=$(ls -1t "$FOLDER" | tail -n 1)
  echo "Deleting old file: $oldest_file"
  rm "$FOLDER/$oldest_file"
  folder_size_mb=$(du -sm "$FOLDER" | cut -f1)
done

# === DETECT THE SCREEN DEVICE INDEX ===
VIDEO_DEVICE_INDEX=$(ffmpeg -f avfoundation -list_devices true -i "" 2>&1 \
  | grep -i "Capture screen" \
  | sed -E 's/.*\[(.*)\].*/\1/' \
  | head -n 1)

if [ -z "$VIDEO_DEVICE_INDEX" ]; then
  echo "❌ Could not find the 'Capture screen' device"
  exit 1
fi

echo "✅ Found screen device: [$VIDEO_DEVICE_INDEX]"

# === START RECORDING ===
echo "🎥 Starting screen recording to: $FULL_PATH"

# without compression
#ffmpeg -f avfoundation -framerate 30 -i "${VIDEO_DEVICE_INDEX}:none" "$FULL_PATH" > /dev/null 2>&1 &

# compression options; for {-crf 28} the ratio is about {0.7}
#-crf 28 — stronger compression (smaller size, lower quality)
#-preset veryfast — faster, slightly larger size
# You can experiment with crf:
#crf 23 — good quality
#crf 28 — smaller size
#crf 30+ — may already be quite blurry

ffmpeg -t 02:30:00 -f avfoundation -framerate 30 -i "${VIDEO_DEVICE_INDEX}:none" \
  -vcodec libx264 -preset veryfast -crf 28 -tune zerolatency \
  "$FULL_PATH" > /dev/null 2>&1 &

echo $! > /tmp/screen_recording_pid.txt

