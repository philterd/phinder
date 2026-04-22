#!/bin/bash -e

echo ""
echo "----------------------------------------------------"
echo "Running Phinder with default policy (Email detection)"
echo "----------------------------------------------------"
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i input.txt

echo ""
echo "----------------------------------------------------"
echo "Running Phinder with a custom policy (policy.json)"
echo "----------------------------------------------------"
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i input.txt -p policy.json
