#!/usr/bin/env bash
# Build all Job Portal Docker images via Google Jib
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MVNW="$ROOT/services/job-user-service/mvnw"
chmod +x "$MVNW" 2>/dev/null || true

MODULES=(
  cloud/eureka-server
  cloud/config-server
  cloud/api-gateway
  services/job-user-service
  services/job-portal-company-service
  services/job-portal-job-service
  services/job-portal-resume-service
  services/job-portal-application-service
  services/job-portal-preferences
  services/job-portal-ai-service
)

echo "Building common-lib first..."
"$MVNW" -f pom.xml -pl common-lib -am package -DskipTests

for module in "${MODULES[@]}"; do
  echo ""
  echo "=== Jib dockerBuild: $module ==="
  "$MVNW" -f pom.xml -pl "$module" -am package jib:dockerBuild -DskipTests
done

echo ""
echo "Done. Images:"
docker images 'job-portal/*'
