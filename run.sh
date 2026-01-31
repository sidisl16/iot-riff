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
# Load Vault Token
if [ -f "vault-init.json" ]; then
    echo "Loading Vault Token from vault-init.json..."
    export VAULT_TOKEN=$(python3 -c "import sys, json; print(json.load(open('vault-init.json'))['root_token'])")
elif [ -f "scripts/vault-init.json" ]; then
     echo "Loading Vault Token from scripts/vault-init.json..."
     export VAULT_TOKEN=$(python3 -c "import sys, json; print(json.load(open('scripts/vault-init.json'))['root_token'])")
else
     echo "Warning: vault-init.json not found. Defaulting to 'root' token (may fail)."
     export VAULT_TOKEN="${VAULT_TOKEN:-root}"
fi

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

# Ensure port 8080 is free
APP_PORT=8080
if lsof -i :$APP_PORT > /dev/null; then
    echo "Port $APP_PORT is in use. Killing blocking process..."
    lsof -t -i :$APP_PORT | xargs kill -9
    sleep 2
    echo "Port $APP_PORT cleared."
fi

java -jar "$PROJECT_DIR/target/iot-riff-0.1.jar" &
APP_PID=$!

echo "Application running with PID: $APP_PID"
echo "Press Ctrl+C to stop application and infrastructure."

# Wait for application to finish
wait $APP_PID
