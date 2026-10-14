#!/bin/bash

if [ -f /tmp/screen_recording_pid.txt ]; then
  PID=$(cat /tmp/screen_recording_pid.txt)
  echo "Stopping recording with PID $PID"
  kill "$PID"
  rm /tmp/screen_recording_pid.txt
else
  echo "No active recording"
fi
