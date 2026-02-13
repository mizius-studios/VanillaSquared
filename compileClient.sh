#!/usr/bin/env bash
set -euo pipefail

DEFAULT_DEST_DIR="/Users/jan.kramer/Library/Application Support/gg.norisk.NoRiskClientV3/profiles/MZS 1.21.11/mods/nrc-1.21.11-fabric/"

if [ "$#" -gt 1 ]; then
  echo "Usage $0 [destination_folder]"
  echo "If omitted, defaults to."
  echo "  $DEFAULT_DEST_DIR"
  exit 1
fi

DEST_DIR="${1:-$DEFAULT_DEST_DIR}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

mkdir -p "$DEST_DIR"

echo "Building mod..."
(cd "$SCRIPT_DIR" && ./gradlew build)

JAR_PATH="$(find "$SCRIPT_DIR/build/libs" -maxdepth 1 -type f -name '*.jar' \
  ! -name '*-sources.jar' \
  ! -name '*-dev.jar' \
  ! -name '*-javadoc.jar' \
  | sort | tail -n 1)"

if [ -z "$JAR_PATH" ]; then
  echo "No built mod JAR found in $SCRIPT_DIR/build/libs"
  exit 1
fi

cp "$JAR_PATH" "$DEST_DIR/"

echo "Copied $(basename "$JAR_PATH")"
echo "To $DEST_DIR"
