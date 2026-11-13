#!/usr/bin/env bash
# Test engine standalone — cần kotlinc + java
set -euo pipefail
if [ ! -f "app/src/main/assets/champions.json" ]; then
  echo "ERROR: champions.json chưa có"; exit 1
fi
if ! command -v kotlinc >/dev/null 2>&1; then
  echo "ERROR: kotlinc chưa cài. Chạy: pkg install kotlin"; exit 1
fi
GSON_JAR="$HOME/.gson/gson.jar"
if [ ! -f "$GSON_JAR" ]; then
  echo "==> Download gson.jar"
  mkdir -p "$HOME/.gson"
  curl -sSL -o "$GSON_JAR" https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar
fi
OUT="build/engine-test"; mkdir -p "$OUT"
kotlinc \
  app/src/main/java/com/penguin/lqban/data/Champion.kt \
  app/src/main/java/com/penguin/lqban/engine/BanScorer.kt \
  tools/test-engine/TestEngine.kt \
  -classpath "$GSON_JAR" -include-runtime -d "$OUT/test-engine.jar"
java -cp "$OUT/test-engine.jar:$GSON_JAR" TestEngineKt
