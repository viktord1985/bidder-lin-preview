#!/bin/bash

osascript -e 'tell application "Google Chrome" to quit'
sleep 2

if pgrep -x "Google Chrome" > /dev/null
then
    echo "Chrome is still running. Forcing quit..."
    pkill -9 -x "Google Chrome"
else
    echo "Chrome closed successfully."
fi
