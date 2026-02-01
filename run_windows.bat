:: windows 11
@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo Raft Cluster - Basic Implementation
echo ==========================================

:: Ensure bin directory exists
if not exist bin mkdir bin

:: Check if Java is installed
:: thx stack overflow for this one.
:: https://stackoverflow.com/questions/44484971/batch-figure-out-if-java-is-installed-or-not
java -version >nul 2>&1
if %errorlevel% == 1 (
    echo [ERROR] Java is not installed or we cannot find it in your path.
    pause
    exit /b 1
)

:: recompile Java files
echo Compiling source files...
javac -d bin src\raft_demo\*.java

if %errorlevel% == 1 (
    echo.
    echo [ERROR] Compilation failed Please check your code if you make a change. Also make sure this is the correct path: %cd%
    pause
    exit /b %errorlevel%
)

echo Compilation successful.
echo ==========================================

:: Start 3 Server Nodes in separate windows
echo Starting Raft Cluster Nodes...
echo [1/3] Node 1 (Port 9102)
start "Raft Node 1" cmd /k "java -cp bin raft_demo.RaftServer 1"

echo [2/3] Node 2 (Port 9103)
start "Raft Node 2" cmd /k "java -cp bin raft_demo.RaftServer 2"

echo [3/3] Node 3 (Port 9104)
start "Raft Node 3" cmd /k "java -cp bin raft_demo.RaftServer 3"

echo Starting. Press any key to close this window.
pause > nul
