#!/bin/bash

if [ -z "$1" ]; then
  echo "Specify the execution duration (in milliseconds)."
  exit 1
fi

DURATION_MS=$1
DURATION=$(echo "scale=6; $DURATION_MS / 1000" | bc)
FPS=10
FRAME_DELAY=$(echo "scale=6; 1 / $FPS" | bc)

# Pick the command for high-res time
if command -v gdate &> /dev/null; then
  DATE_CMD="gdate +%s.%N"
else
  echo "⚠️  gdate not found. Install with: brew install coreutils"
  echo "⏳  Using less precise time measurement (seconds without milliseconds)"
  DATE_CMD="date +%s"
fi

# Screen resolution (set manually)
SCREEN_WIDTH=1920
SCREEN_HEIGHT=1080
CENTER_X=$((SCREEN_WIDTH / 2))
CENTER_Y=$((SCREEN_HEIGHT / 2))
BASE_RADIUS=200

echo "Drawing a figure eight for ${DURATION_MS} ms..."

START_TIME=$(eval "$DATE_CMD")

for ((i = 0; ; i++)); do
    CURRENT_TIME=$(eval "$DATE_CMD")
    ELAPSED=$(echo "$CURRENT_TIME - $START_TIME" | bc)

    if (( $(echo "$ELAPSED >= $DURATION" | bc -l) )); then
        break
    fi

    RADIUS=$((BASE_RADIUS + RANDOM % 21 - 10))
    T=$(echo "scale=6; $i * 6.2832 / ($DURATION * $FPS)" | bc)

    SIN_T=$(echo "s($T)" | bc -l)
    COS_T=$(echo "c($T)" | bc -l)

    X_OFFSET=$(echo "$SIN_T" | bc -l)
    Y_OFFSET=$(echo "$SIN_T * $COS_T" | bc -l)

    RAND_X=$((RANDOM % 11 - 5))
    RAND_Y=$((RANDOM % 11 - 5))

    X=$(printf "%.0f" "$(echo "$CENTER_X + ($RADIUS * $X_OFFSET) + $RAND_X" | bc -l)")
    Y=$(printf "%.0f" "$(echo "$CENTER_Y + ($RADIUS * $Y_OFFSET) + $RAND_Y" | bc -l)")

    cliclick m:$X,$Y > /dev/null

    TARGET_NEXT_TIME=$(echo "$START_TIME + ($i + 1) * $FRAME_DELAY" | bc)
    while :; do
        CURRENT_TIME=$(eval "$DATE_CMD")
        if (( $(echo "$CURRENT_TIME >= $TARGET_NEXT_TIME" | bc -l) )); then
            break
        fi
        sleep 0.001
    done
done

echo "Done!"
