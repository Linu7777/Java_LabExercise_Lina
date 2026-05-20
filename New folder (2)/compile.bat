@echo off
echo =============================================
echo     Java Poker GUI - Full Compiler + Runner
echo =============================================

echo.
echo Cleaning old files...
del *.class 2>nul
del PokerGame.jar 2>nul

echo.
echo Compiling...
javac *.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Compilation FAILED!
    pause
    exit /b 1
)

echo ✅ Compilation successful!

echo.
echo Creating JAR with proper manifest...
jar cfm PokerGame.jar manifest.txt *.class

if %ERRORLEVEL% NEQ 0 (
    echo ❌ JAR creation failed!
    pause
    exit /b 1
)

echo ✅ JAR created successfully!
echo.
echo Launching Game...
echo.
java -jar PokerGame.jar

pause