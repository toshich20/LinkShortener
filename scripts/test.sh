#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="/tmp/opencode/linkshortener-build"
CP="$HOME/.m2/repository/org/junit/jupiter/junit-jupiter/5.14.0/junit-jupiter-5.14.0.jar:$HOME/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.14.0/junit-jupiter-api-5.14.0.jar:$HOME/.m2/repository/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar:$HOME/.m2/repository/org/junit/platform/junit-platform-commons/1.14.0/junit-platform-commons-1.14.0.jar:$HOME/.m2/repository/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar:$HOME/.m2/repository/org/junit/jupiter/junit-jupiter-params/5.14.0/junit-jupiter-params-5.14.0.jar:$HOME/.m2/repository/org/junit/jupiter/junit-jupiter-engine/5.14.0/junit-jupiter-engine-5.14.0.jar:$HOME/.m2/repository/org/junit/platform/junit-platform-engine/1.14.0/junit-platform-engine-1.14.0.jar:$HOME/.m2/repository/org/junit/platform/junit-platform-launcher/1.14.0/junit-platform-launcher-1.14.0.jar"
rm -rf "$TMP"
mkdir -p "$TMP/classes" "$TMP/test-classes"
find "$ROOT/src" -name '*.java' | sort > "$TMP/srcfiles.txt"
javac -d "$TMP/classes" @"$TMP/srcfiles.txt"
cp "$ROOT/src/config.properties" "$TMP/classes/config.properties"
find "$ROOT/test" -name '*.java' | sort > "$TMP/testfiles.txt"
javac -cp "$TMP/classes:$CP" -d "$TMP/test-classes" @"$TMP/testfiles.txt"
cat > "$TMP/TestRunner.java" << 'JAVA'
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
public class TestRunner {
  public static void main(String[] args) {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectPackage("service")).build();
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    var launcher = LauncherFactory.create();
    launcher.registerTestExecutionListeners(listener);
    launcher.execute(request);
    TestExecutionSummary s = listener.getSummary();
    System.out.println("FOUND=" + s.getTestsFoundCount());
    System.out.println("SUCCEEDED=" + s.getTestsSucceededCount());
    System.out.println("FAILED=" + s.getTestsFailedCount());
    s.getFailures().forEach(f -> { System.out.println("FAILURE: " + f.getTestIdentifier().getDisplayName()); f.getException().printStackTrace(); });
    if (s.getTestsFailedCount() > 0) System.exit(1);
  }
}
JAVA
javac -cp "$TMP/classes:$TMP/test-classes:$CP" -d "$TMP" "$TMP/TestRunner.java"
java -cp "$TMP:$TMP/classes:$TMP/test-classes:$CP" TestRunner
