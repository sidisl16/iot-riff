#!/bin/bash

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SETUP_SCRIPT="$PROJECT_DIR/scripts/setup-tools.sh"

# Cleanup Function
cleanup() {
    echo ""
    echo "=========================================="
    echo "Stopping Application and Infrastructure..."
    echo "=========================================="
    
    # Kill the child application process if it's running
    if [ -n "$APP_PID" ]; then
        echo "Stopping Java Application (PID: $APP_PID)..."
        kill $APP_PID 2>/dev/null
    fi

    # Stop Infrastructure
    if [ -f "$SETUP_SCRIPT" ]; then
        "$SETUP_SCRIPT" stop
    else
        echo "Warning: Setup script not found for cleanup."
    fi
    exit
}

# Trap Signals
trap cleanup INT TERM

# Ensure Infrastructure is Up
echo "=========================================="
echo "Starting Infrastructure..."
echo "=========================================="
if [ -f "$SETUP_SCRIPT" ]; then
    "$SETUP_SCRIPT" start
else
    echo "Warning: Setup script not found at $SETUP_SCRIPT"
    exit 1
fi

# Set Environment Variables
export VAULT_TOKEN="${VAULT_TOKEN:-root}"
export VAULT_ADDR='http://127.0.0.1:8200'

# Build
echo ""
echo "=========================================="
echo "Building Project..."
echo "=========================================="
"$PROJECT_DIR/mvnw" clean install

if [ $? -ne 0 ]; then
    echo "Build failed. Exiting."
    exit 1
fi

# Run
echo ""
echo "=========================================="
echo "Starting Application..."
echo "=========================================="
java -jar "$PROJECT_DIR/target/iot-riff-0.1.jar" &
APP_PID=$!

echo "Application running with PID: $APP_PID"
echo "Press Ctrl+C to stop application and infrastructure."

# Wait for application to finish
wait $APP_PID
