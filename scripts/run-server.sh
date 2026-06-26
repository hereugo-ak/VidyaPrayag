#!/bin/bash
# Run the VidyaPrayag Ktor backend (fast server-only mode)
# Usage: ./scripts/run-server.sh

cd "$(dirname "$0")/.."

echo "Starting VidyaPrayag server (server-only mode)..."
echo "Server will be available at http://0.0.0.0:8080"
echo "Press Ctrl+C to stop."
echo ""

./gradlew :server:run -Pserver-only=true
