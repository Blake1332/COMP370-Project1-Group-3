#!/bin/bash

echo "=========================================="
echo "Raft Cluster - GUI"
echo "=========================================="

mkdir -p bin
mkdir -p logs

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or we cannot find it in your PATH."
    exit 1
fi

echo "Launching GUI..."
java -cp bin raft_demo.GUI
