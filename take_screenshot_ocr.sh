#!/bin/bash

# Define the directory where you want to save the screenshot
SAVE_DIR="export/screen_detect"

mkdir -p "$SAVE_DIR" > /dev/null

# Define the filename with the current timestamp
FILENAME="screenshot_$(date +%Y-%m-%d_%H-%M-%S).png"
# Full path for the screenshot file
FULL_PATH="$SAVE_DIR/$FILENAME"

# If 4 arguments (L T W H) are passed — use them
if [ $# -eq 4 ]; then
    L=$1
    T=$2
    W=$3
    H=$4
else
    # 1) Get the bounds of the front Chrome window
    # Returns: left,top,right,bottom
    read L T R B < <(osascript -e 'tell application "Google Chrome" to get bounds of front window' \
                     | tr -d '{}' | tr ',' ' ')

    # 2) Compute width/height
    W=$((R - L))
    H=$((B - T))
fi

# Take the screenshot and save it
screencapture -R"${L},${T},${W},${H}" -x "$FULL_PATH"

# Check if the screenshot was successfully taken
if [ -f "$FULL_PATH" ]; then
    echo "$FULL_PATH"
else
    echo "Failed"
fi

# Define the size limit 512Mb
SIZE_LIMIT=$((512 * 1024))

# Calculate the size of the folder in kilobytes
FOLDER_SIZE=$(du -sk "$SAVE_DIR" | awk '{print $1}')

# Check if the folder size exceeds the limit
if [ "$FOLDER_SIZE" -gt "$SIZE_LIMIT" ]; then
    echo "Folder size exceeds 1GB, deleting old files..." > /dev/null
    # Find and delete the oldest files until the folder size is under the limit
    while [ "$FOLDER_SIZE" -gt "$SIZE_LIMIT" ]; do
        # Find the oldest file in the folder and delete it
        OLDEST_FILE=$(find "$SAVE_DIR" -type f -exec ls -t {} + | tail -n 1)
        if [ -n "$OLDEST_FILE" ]; then
            echo "Deleting $OLDEST_FILE" > /dev/null
            rm "$OLDEST_FILE"
        else
            echo "No files found to delete." > /dev/null
            break
        fi

        # Recalculate the folder size in kilobytes
        FOLDER_SIZE=$(du -sk "$SAVE_DIR" | awk '{print $1}')
    done
    echo "Old files deleted to reduce folder size." > /dev/null
else
    echo "Folder size is within the limit." > /dev/null
fi