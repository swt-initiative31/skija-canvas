#!/bin/bash
cd "C:/SAPDevelop/git/skija-canvas"

# Run the test using Maven
mvn test -pl tests/org.eclipse.swt.skia.tests \
  -Dtest=Test_org_eclipse_swt_skia_drawString \
  -DfailIfNoTests=false \
  2>&1 | tee test_output.log

# Show the result
echo "Test execution completed. Check test_output.log for details."
