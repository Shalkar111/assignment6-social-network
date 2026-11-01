#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p .deps build/classes build/test-classes
JUNIT_JAR="${JUNIT_JAR:-.deps/junit-platform-console-standalone-1.10.2.jar}"
if [ ! -f "$JUNIT_JAR" ]; then
  curl -fL --retry 2 https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.2/junit-platform-console-standalone-1.10.2.jar -o "$JUNIT_JAR"
fi
find src/main/java -name '*.java' | sort > build/main-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/main-sources.txt
javac --release 17 -encoding UTF-8 -cp "build/classes:$JUNIT_JAR" -d build/test-classes @build/test-sources.txt
java -jar "$JUNIT_JAR" execute --class-path build/classes:build/test-classes --scan-class-path --fail-if-no-tests
