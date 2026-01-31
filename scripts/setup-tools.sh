#!/bin/bash

# Configuration
TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Vault Config
VAULT_VERSION="1.21.2"
VAULT_PORT="8200"
VAULT_TOKEN="root"
VAULT_DIR="$TOOLS_DIR/vault_${VAULT_VERSION}_darwin_arm64"
VAULT_BIN="$VAULT_DIR/vault"

# Kafka Config
SCALA_VERSION="2.13"
KAFKA_VERSION="4.1.1" # Using version found in directory listing
KAFKA_DIR="$TOOLS_DIR/kafka_${SCALA_VERSION}-${KAFKA_VERSION}"
KAFKA_BIN="$KAFKA_DIR/bin"
KAFKA_PORT="9092"
KAFKA_LOG_DIR="/tmp/kraft-combined-logs" # Default in server.properties

# MongoDB Config
MONGO_VERSION="8.2.3"
MONGO_ARCH="arm64" 
MONGO_DIR_NAME="mongodb-macos-aarch64--${MONGO_VERSION}"
MONGO_DIR="$TOOLS_DIR/$MONGO_DIR_NAME"

# Fallback MongoDB Directory Check
if [ ! -d "$MONGO_DIR" ]; then
    MONGO_DIR_NAME_STD="mongodb-macos-aarch64-${MONGO_VERSION}"
    if [ -d "$TOOLS_DIR/$MONGO_DIR_NAME_STD" ]; then
         MONGO_DIR="$TOOLS_DIR/$MONGO_DIR_NAME_STD"
    fi
fi

MONGO_BIN="$MONGO_DIR/bin"
MONGO_PORT="27017"
MONGO_DB_PATH="$TOOLS_DIR/mongodb-data"
MONGO_LOG="$TOOLS_DIR/mongo.log"

echo "Tools Directory: $TOOLS_DIR"

# Ensure Tools Directory exists
if [ ! -d "$TOOLS_DIR" ]; then
  echo "Error: Tools directory does not exist at $TOOLS_DIR"
  exit 1
fi

start() {
    # ==========================================
    # MongoDB Setup (Started First for Vault Persistence)
    # ==========================================
    echo "----------------------------------------------------------------"
    echo "Checking MongoDB..."

    if [ ! -d "$MONGO_DIR" ]; then
        echo "MongoDB directory not found at $MONGO_DIR."
        TGZ_FILE="$TOOLS_DIR/mongodb-macos-${MONGO_ARCH}-${MONGO_VERSION}.tgz"
        
        if [ ! -f "$TGZ_FILE" ]; then
            echo "Downloading MongoDB $MONGO_VERSION..."
            curl -o "$TGZ_FILE" "https://fastdl.mongodb.org/osx/mongodb-macos-${MONGO_ARCH}-${MONGO_VERSION}.tgz"
        fi
        
        echo "Extracting MongoDB..."
        tar -xzf "$TGZ_FILE" -C "$TOOLS_DIR"
        
        EXTRACTED_DIR=$(find "$TOOLS_DIR" -maxdepth 1 -type d -name "mongodb-macos-aarch64*${MONGO_VERSION}" | head -n 1)
        if [ -n "$EXTRACTED_DIR" ]; then
            MONGO_DIR="$EXTRACTED_DIR"
            MONGO_BIN="$MONGO_DIR/bin"
        fi
    else
        echo "MongoDB directory found."
    fi

    if lsof -i :$MONGO_PORT > /dev/null; then
        echo "MongoDB is already running on port $MONGO_PORT."
    else
        echo "Starting MongoDB..."
        
        # Create data directory
        if [ ! -d "$MONGO_DB_PATH" ]; then
            echo "Creating data directory at $MONGO_DB_PATH..."
            mkdir -p "$MONGO_DB_PATH"
        fi
        
        # Start mongod (Background)
        "$MONGO_BIN/mongod" --dbpath "$MONGO_DB_PATH" > "$MONGO_LOG" 2>&1 &
        
        sleep 5
        if lsof -i :$MONGO_PORT > /dev/null; then
            echo "MongoDB started successfully."
        else
            echo "Error starting MongoDB. Check logs in $MONGO_LOG"
            exit 1
        fi
    fi

    # ==========================================
    # HashiCorp Vault Setup
    # ==========================================
    echo "----------------------------------------------------------------"
    echo "Checking HashiCorp Vault..."

    if [ ! -f "$VAULT_BIN" ]; then
        echo "Vault binary not found at $VAULT_BIN."
        
        ZIP_FILE="$TOOLS_DIR/vault_${VAULT_VERSION}_darwin_arm64.zip"
        if [ ! -f "$ZIP_FILE" ]; then
             echo "Downloading Vault $VAULT_VERSION..."
             curl -o "$ZIP_FILE" "https://releases.hashicorp.com/vault/${VAULT_VERSION}/vault_${VAULT_VERSION}_darwin_arm64.zip"
        fi
        
        echo "Unzipping Vault..."
        unzip -o "$ZIP_FILE" -d "$VAULT_DIR"
        chmod +x "$VAULT_BIN"
    else
        echo "Vault binary found."
    fi

    if ! lsof -i :$VAULT_PORT > /dev/null; then
        echo "Starting Vault..."
        
        # Ensure data directory exists for Raft
        VAULT_DATA_DIR="$TOOLS_DIR/vault-data"
        if [ ! -d "$VAULT_DATA_DIR" ]; then
            mkdir -p "$VAULT_DATA_DIR"
        fi
        
        VAULT_CONFIG="$(dirname "${BASH_SOURCE[0]}")/vault-config.hcl"
        
        # Start Vault
        cd "$TOOLS_DIR" && "$VAULT_BIN" server -config="$VAULT_CONFIG" > "$TOOLS_DIR/vault.log" 2>&1 &
        sleep 5
    else
        echo "Vault is already running on port $VAULT_PORT."
    fi

    if lsof -i :$VAULT_PORT > /dev/null; then
        echo "Vault is running."
        export VAULT_ADDR="http://127.0.0.1:$VAULT_PORT"
        
        # Initialize Vault if needed
        # We check specific status code or output. grep for "Initialized.*true"
        if ! "$VAULT_BIN" status | grep -q "Initialized.*true"; then
             echo "Initializing Vault..."
             # Initialize with single key for dev simplicity
             INIT_OUTPUT=$("$VAULT_BIN" operator init -key-shares=1 -key-threshold=1 -format=json)
             echo "$INIT_OUTPUT" > "$TOOLS_DIR/vault-init.json"
             echo "Vault initialized. Keys saved to $TOOLS_DIR/vault-init.json"
        fi
        
        # Unseal Vault
        if "$VAULT_BIN" status | grep -q "Sealed.*true"; then
             echo "Unsealing Vault..."
             if [ -f "$TOOLS_DIR/vault-init.json" ]; then
                 if command -v python3 &>/dev/null; then
                     UNSEAL_KEY=$(python3 -c "import sys, json; print(json.load(sys.stdin)['unseal_keys_b64'][0])" < "$TOOLS_DIR/vault-init.json")
                     "$VAULT_BIN" operator unseal "$UNSEAL_KEY"
                 else
                     echo "Error: python3 not found. Cannot extract unseal key from JSON."
                     # Fallback could be added here but Python3 is standard on macOS devs
                 fi
             else
                 echo "Error: Vault is sealed but vault-init.json not found. Cannot auto-unseal."
             fi
        fi

        # Get Root Token for Setup
        if [ -f "$TOOLS_DIR/vault-init.json" ]; then
            if command -v python3 &>/dev/null; then
                VAULT_TOKEN=$(python3 -c "import sys, json; print(json.load(sys.stdin)['root_token'])" < "$TOOLS_DIR/vault-init.json")
                export VAULT_TOKEN
            else
                 echo "Error: python3 not found. Cannot extract root token."
            fi
        else
            echo "Warning: Could not retrieve root token. Setup tasks might fail."
        fi

        # Enable KV secret path
        if ! "$VAULT_BIN" secrets list 2>/dev/null | grep -q "^secret/"; then
            echo "Enabling KV secrets engine at secret/..."
            "$VAULT_BIN" secrets enable -path=secret kv
        else
            echo "Secrets engine at secret/ already enabled."
        fi

        # Apply Password Policy
        POLICY_FILE="$(dirname "${BASH_SOURCE[0]}")/policy.hcl"
        if [ -f "$POLICY_FILE" ]; then
             echo "Applying Vault password policy from $POLICY_FILE..."
             "$VAULT_BIN" write sys/policies/password/iot-policy policy=@"$POLICY_FILE"
        else
             echo "Warning: Policy file not found at $POLICY_FILE"
        fi
    else
        echo "Error: Vault is not running. Check $TOOLS_DIR/vault.log"
    fi

    # ==========================================
    # Apache Kafka Setup (KRaft Mode)
    # ==========================================
    echo "----------------------------------------------------------------"
    echo "Checking Apache Kafka..."

    if [ ! -d "$KAFKA_DIR" ]; then
        echo "Kafka directory not found at $KAFKA_DIR."
        TGZ_FILE="$TOOLS_DIR/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
        
        if [ ! -f "$TGZ_FILE" ]; then
            echo "Downloading Kafka $KAFKA_VERSION..."
            # Try generic Apache mirror or archive
            curl -o "$TGZ_FILE" "https://downloads.apache.org/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz"
        fi
        
        echo "Extracting Kafka..."
        tar -xzf "$TGZ_FILE" -C "$TOOLS_DIR"
    else
        echo "Kafka directory found."
    fi

    if lsof -i :$KAFKA_PORT > /dev/null; then
        echo "Kafka is already running on port $KAFKA_PORT."
    else
        echo "Starting Kafka in KRaft mode..."
        
        # Check if formatted
        if [ ! -f "$KAFKA_LOG_DIR/meta.properties" ]; then
            echo "Formatting Kafka storage..."
            CLUSTER_ID=$($KAFKA_BIN/kafka-storage.sh random-uuid)
            $KAFKA_BIN/kafka-storage.sh format -t $CLUSTER_ID -c $KAFKA_DIR/config/server.properties
        fi
        
        "$KAFKA_BIN/kafka-server-start.sh" -daemon "$KAFKA_DIR/config/server.properties"
        
        sleep 5
        if lsof -i :$KAFKA_PORT > /dev/null; then
            echo "Kafka started successfully."
        else
            echo "Error starting Kafka. Check logs in $KAFKA_DIR/logs/server.log"
        fi
    fi

    # ==========================================
    # Summary & Shell Persistence
    # ==========================================
    echo "----------------------------------------------------------------"
    echo "Setup Complete."
    
    # Define exports
    EXPORTS_BLOCK="# IoT-Riff Dev Tools
export VAULT_TOKEN=$VAULT_TOKEN
export VAULT_ADDR='http://127.0.0.1:$VAULT_PORT'
export KAFKA_HOME='$KAFKA_DIR'
export PATH=\"\$PATH:$MONGO_BIN:$VAULT_DIR\""

    # Detect Shell Config
    SHELL_RC=""
    if [[ "$SHELL" == */zsh ]]; then
        SHELL_RC="$HOME/.zshrc"
    elif [[ "$SHELL" == */bash ]]; then
        if [ -f "$HOME/.bash_profile" ]; then
            SHELL_RC="$HOME/.bash_profile"
        else
            SHELL_RC="$HOME/.bashrc"
        fi
    fi

    # Append to Shell Config if valid and not already present
    # Append to Shell Config if valid and not already present
    if [ -n "$SHELL_RC" ] && [ -f "$SHELL_RC" ]; then
        if ! grep -q "IoT-Riff Dev Tools" "$SHELL_RC"; then
            echo "" >> "$SHELL_RC"
            echo "$EXPORTS_BLOCK" >> "$SHELL_RC"
            echo "Added configuration to $SHELL_RC"
        else
            echo "Configuration block found in $SHELL_RC"
            # Update VAULT_TOKEN if it differs
            # We use sed to replace the specific line.
            # MacOS sed requires -i '' for in-place without backup
            if [[ "$OSTYPE" == "darwin"* ]]; then
                 sed -i '' "s|^export VAULT_TOKEN=.*|export VAULT_TOKEN=$VAULT_TOKEN|" "$SHELL_RC"
            else
                 sed -i "s|^export VAULT_TOKEN=.*|export VAULT_TOKEN=$VAULT_TOKEN|" "$SHELL_RC"
            fi
            echo "Updated VAULT_TOKEN in $SHELL_RC"
        fi
        echo ""
        echo "Run 'source $SHELL_RC' to update your current shell."
    else
        echo ""
        echo "Could not detect shell config file. Please add manually:"
        echo "$EXPORTS_BLOCK"
    fi
}

stop_process_on_port() {
    local port=$1
    local name=$2
    local pid=$(lsof -t -i :$port)

    if [ -n "$pid" ]; then
        echo "Stopping $name (PID: $pid) on port $port..."
        kill $pid
        
        # Wait for process to exit
        local count=0
        while kill -0 $pid 2>/dev/null; do
            sleep 1
            count=$((count+1))
            if [ $count -ge 10 ]; then
                echo "Force killing $name..."
                kill -9 $pid
                break
            fi
        done
        echo "$name stopped."
    else
        echo "$name is not running on port $port."
    fi
}

stop() {
    echo "=========================================="
    echo "Stopping Infrastructure..."
    echo "=========================================="
    stop_process_on_port $VAULT_PORT "Vault"
    stop_process_on_port $KAFKA_PORT "Kafka"
    stop_process_on_port $MONGO_PORT "MongoDB"
}

# Main Execution Switch
case "$1" in
    stop)
        stop
        ;;
    start)
        start
        ;;
    *)
        start
        ;;
esac
