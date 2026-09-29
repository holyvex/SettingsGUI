#!/usr/bin/env bash
set -e
mvn -q clean package
echo "Done! Your jar is: target/SettingsGUI-1.0.0.jar"
