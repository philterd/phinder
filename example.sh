#!/bin/bash -e

#echo ""
#echo "----------------------------------------------------"
#echo "Running Phinder with default policy (Email detection)"
#echo "----------------------------------------------------"
#java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -r report.json -f json

#echo ""
#echo "----------------------------------------------------"
#echo "Running Phinder with a custom policy (policy.json)"
#echo "----------------------------------------------------"
#java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -p src/test/resources/policy.json
#
#echo ""
#echo "----------------------------------------------------"
#echo "Running Phinder with a PDF file (input.pdf)"
#echo "----------------------------------------------------"
#java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.pdf

#echo ""
#echo "----------------------------------------------------"
#echo "Running Phinder with multiple files and generating a PDF report"
#echo "----------------------------------------------------"
#java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/input.pdf -r report.pdf -f pdf

echo ""
echo "----------------------------------------------------"
echo "Running Phinder with custom PII weights (weights.json)"
echo "----------------------------------------------------"

# Create an example weights.json
cat <<EOF > weights.json
{
  "email-address": 5.0,
  "ip-address": 2.5
}
EOF

java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -w weights.json

rm weights.json