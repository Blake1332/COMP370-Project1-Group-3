#!/bin/bash

echo "=========================================="
echo "Raft Cluster - Basic Implementation"
echo "=========================================="

# Ensure bin directory exists
mkdir -p bin

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or we cannot find it in your PATH."
    exit 1
fi

# Recompile Java files
echo "Compiling source files..."
javac -d bin src/raft_demo/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERROR] Compilation failed. Please check your code if you made a change. Also make sure this is the correct path: $(pwd)"
    exit $?
fi

echo "Compilation successful."
echo "=========================================="

# Start 3 Server Nodes in background processes
echo "Starting Raft Cluster Nodes..."

echo "[1/3] Node 1 (Port 9102)"
java -cp bin raft_demo.RaftServer 1 > /dev/null 2>&1 &
NODE1_PID=$!
echo "Node 1 started with PID $NODE1_PID"

echo "[2/3] Node 2 (Port 9103)"
java -cp bin raft_demo.RaftServer 2 > /dev/null 2>&1 &
NODE2_PID=$!
echo "Node 2 started with PID $NODE2_PID"

echo "[3/3] Node 3 (Port 9104)"
java -cp bin raft_demo.RaftServer 3 > /dev/null 2>&1 &
NODE3_PID=$!
echo "Node 3 started with PID $NODE3_PID"

echo "=========================================="
echo "All nodes started in background."
echo "View logs with: tail -f logs/node_*.log"
echo "Stop nodes with: kill $NODE1_PID $NODE2_PID $NODE3_PID"
echo "=========================================="