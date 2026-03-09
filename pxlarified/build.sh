#!/usr/bin/env bash
set -euo pipefail

TARGET_DIR="/mnt/c/Users/jan/AppData/Roaming/norisk/NoRiskClientV3/data/profiles/Fabric 1.21.11(2)/mods/nrc-1.21.11-fabric"

./gradlew build --no-daemon

JAR_FILE="$(ls -t build/libs/vsq--2.7.0-STABLE.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "$JAR_FILE" ]]; then
    echo "No jar found in build/libs."
    exit 1
fi

mkdir -p "$TARGET_DIR"
cp -f "$JAR_FILE" "$TARGET_DIR/"

echo "Deployed $JAR_FILE to $TARGET_DIR."
