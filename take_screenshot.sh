#!/bin/bash

# Define the directory where you want to save the screenshot
SAVE_DIR="export/screen_logs"
# Define the filename with the current timestamp
FILENAME="$1_screenshot_$(date +%Y-%m-%d_%H-%M-%S).png"
# Full path for the screenshot file
FULL_PATH="$SAVE_DIR/$FILENAME"

# Take the screenshot and save it
screencapture -x "$FULL_PATH"

# Check if the screenshot was successfully taken
if [ -f "$FULL_PATH" ]; then
    echo "Screenshot saved to $FULL_PATH"
else
    echo "Failed to take screenshot"
fi

# Define the size limit 512Mb
SIZE_LIMIT=$((512 * 1024))

# Calculate the size of the folder in kilobytes
FOLDER_SIZE=$(du -sk "$SAVE_DIR" | awk '{print $1}')

# Check if the folder size exceeds the limit
if [ "$FOLDER_SIZE" -gt "$SIZE_LIMIT" ]; then
    echo "Folder size exceeds 1GB, deleting old files..."
    # Find and delete the oldest files until the folder size is under the limit
    while [ "$FOLDER_SIZE" -gt "$SIZE_LIMIT" ]; do
        # Find the oldest file in the folder and delete it
        OLDEST_FILE=$(find "$SAVE_DIR" -type f -exec ls -t {} + | tail -n 1)
        if [ -n "$OLDEST_FILE" ]; then
            echo "Deleting $OLDEST_FILE"
            rm "$OLDEST_FILE"
        else
            echo "No files found to delete."
            break
        fi

        # Recalculate the folder size in kilobytes
        FOLDER_SIZE=$(du -sk "$SAVE_DIR" | awk '{print $1}')
    done
    echo "Old files deleted to reduce folder size."
else
    echo "Folder size is within the limit."
fi