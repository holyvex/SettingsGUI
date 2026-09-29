@echo off
echo Building SettingsGUI...
call mvn -q clean package
if errorlevel 1 (echo BUILD FAILED & pause & exit /b 1)
echo.
echo Done! Your jar is: target\SettingsGUI-1.0.0.jar
pause
