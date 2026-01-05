#!/usr/bin/env bash
set -euo pipefail

COVERAGE_LIMIT=0.70

MODULES=(
  "microservices/user-service"
  "microservices/wardrobe-service"
  "microservices/outfit-service"
)

TOTAL_COVERED=0
TOTAL_MISSED=0

declare -a MODULE_SUMMARY_LINES=()

echo "🚀 Running tests with coverage (JaCoCo + Testcontainers)"

for m in "${MODULES[@]}"; do
  echo "=============================="
  echo "Testing $m"
  echo "=============================="

  mvn -f "$m/pom.xml" clean test jacoco:report

  JACOCO_XML="$m/target/site/jacoco/jacoco.xml"

  if [ ! -f "$JACOCO_XML" ]; then
    echo "❌ JaCoCo report not found for $m"
    exit 1
  fi

  COVERED=$(grep -o 'covered="[0-9]*"' "$JACOCO_XML" | sed 's/[^0-9]//g' | awk '{s+=$1} END {print s}')
  MISSED=$(grep -o 'missed="[0-9]*"' "$JACOCO_XML" | sed 's/[^0-9]//g' | awk '{s+=$1} END {print s}')

  TOTAL_COVERED=$((TOTAL_COVERED + COVERED))
  TOTAL_MISSED=$((TOTAL_MISSED + MISSED))

  MODULE_TOTAL=$((COVERED + MISSED))
  MODULE_COVERAGE=$(awk "BEGIN { printf \"%.2f\", $COVERED / $MODULE_TOTAL }")
  SUMMARY_LINE="📦 $m: covered=$COVERED missed=$MISSED coverage=$MODULE_COVERAGE"
  echo "$SUMMARY_LINE"
  MODULE_SUMMARY_LINES+=("$SUMMARY_LINE")
done

TOTAL=$((TOTAL_COVERED + TOTAL_MISSED))
COVERAGE_RAW=$(awk "BEGIN { printf \"%.6f\", $TOTAL_COVERED / $TOTAL }")
COVERAGE=$(awk "BEGIN { printf \"%.2f\", $TOTAL_COVERED / $TOTAL }")

echo "=============================="
echo "📊 TOTAL COVERAGE: $COVERAGE (raw=$COVERAGE_RAW)"
echo "=============================="

if awk "BEGIN {exit !($COVERAGE_RAW >= $COVERAGE_LIMIT)}"; then
  echo "✅ Coverage OK (>= $COVERAGE_LIMIT)"
else
  echo "❌ Coverage FAILED (< $COVERAGE_LIMIT)"
  echo "=============================="
  echo "📦 Per-module summary"
  echo "=============================="
  for line in "${MODULE_SUMMARY_LINES[@]}"; do
    echo "$line"
  done
  exit 1
fi

echo "=============================="
echo "📦 Per-module summary"
echo "=============================="
for line in "${MODULE_SUMMARY_LINES[@]}"; do
  echo "$line"
done
