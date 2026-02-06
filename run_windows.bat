@echo off

echo ==========================================
echo Raft Cluster - GUI
echo ==========================================

:: Ensure bin and logs directories exist
if not exist bin mkdir bin
if not exist logs mkdir logs

:: Check if Java is installed
java -version >nul 2>&1
if %errorlevel% == 1 (
    echo [ERROR] Java is not installed or we cannot find it in your path.
    pause
    exit /b 1
)

:: Compile Java files
echo Compiling source files...
javac -d bin src\raft_demo\*.java

if %errorlevel% == 1 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %errorlevel%
)

echo Compilation successful.
echo ==========================================
echo Launching GUI...
java -cp bin raft_demo.GUI
