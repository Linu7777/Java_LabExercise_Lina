@echo off
echo =====================================
echo     Starting Java Poker GUI
echo =====================================
echo.

echo Running directly (no JAR)...
java PokerGameGUI

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Failed to run.
    echo Make sure you have compiled first.
    pause
)

pause