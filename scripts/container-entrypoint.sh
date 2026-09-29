#!/bin/sh
set -eu
if [ ! -f "$WORLD_FILE" ]; then cp /app/defaults/world.json "$WORLD_FILE"; fi
exec java -jar /app/server.jar
