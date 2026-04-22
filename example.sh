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
echo "Running Phinder with multiple files and generating an HTML report"
echo "----------------------------------------------------"
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml -r report.html -f html

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
echo "Running Phinder with scan logging and skipping unchanged files"
echo "----------------------------------------------------"

echo "First run: generating scan and report.html"
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml --log --format html -r report.html

echo ""
echo "Second run: skipping unchanged files"
java -jar target/phinder-1.0.0-SNAPSHOT.jar -i src/test/resources/input.txt -i src/test/resources/test.eml --skip-unchanged

echo ""
echo "Check report for skipped files count"
grep "Files Skipped" report.txt

# Keep report.html for the user to see
rm weights.json scan.mv.db report.txt