#!/bin/bash

# Find all processes matching "teamviewer" (case-insensitive)
PIDS=$(ps aux | grep -i teamviewer | grep -v grep | awk '{print $2}')

# Check if there are any PIDs found
if [ -z "$PIDS" ]; then
  echo "No TeamViewer processes found."
else
  # Loop through and kill each process
  for PID in $PIDS; do
    echo "Killing process with PID: $PID"
    kill -9 $PID
  done
  echo "All matching processes have been killed."
fi
