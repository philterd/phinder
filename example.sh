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


echo ""
echo "----------------------------------------------------"
echo "Running Phinder with multiple files and generating an HTML and JSON report"
echo "----------------------------------------------------"
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml

echo ""
echo "----------------------------------------------------"
echo "Running Phinder with custom PII weights (weights.json) to affect Magnitude Score"
echo "----------------------------------------------------"

# Create an example weights.json
cat <<EOF > weights.json
{
  "email-address": 5.0,
  "ip-address": 2.5
}
EOF

java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml -w weights.json

echo ""
echo "----------------------------------------------------"
echo "Prioritizing by Risk Score: the report lists only files scoring 20.0 and above"
echo "----------------------------------------------------"

# The console below still lists every file scanned. --min-risk and --sort-by shape the generated
# report, so it is report.html and report.json that hold only the higher-risk file, ranked.
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml --sort-by risk --min-risk 20 --top 5

echo ""
echo "----------------------------------------------------"
echo "Running Phinder with custom PII severities (severities.json) to affect Risk Score"
echo "----------------------------------------------------"

# Create an example severities.json. Severities weigh how sensitive each entity type is, so
# raising one lifts the Risk Score of every file that holds it. Types left out keep their default.
cat <<EOF > severities.json
{
  "email-address": 8.0,
  "state-abbreviation": 0.25
}
EOF

java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml --severities severities.json

#echo ""
#echo "----------------------------------------------------"
#echo "Running Phinder with scan logging and skipping unchanged files"
#echo "----------------------------------------------------"
#
#echo "First run: generating scan and reports"
#java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml --log
#
#echo ""
#echo "Second run: skipping unchanged files"
#java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml --skip-unchanged

# Keep report.html for the user to see
rm weights.json
rm severities.json