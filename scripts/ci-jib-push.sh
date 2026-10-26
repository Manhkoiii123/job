#!/usr/bin/env bash
# Build & push all Job Portal images with Jib (no Docker daemon required for build).
# Used by GitHub Actions CD. Can also run locally after: export DOCKERHUB_USERNAME=... DOCKERHUB_TOKEN=...
#
# Image naming matches scripts/push-images-dockerhub.ps1 + deploy/docker-compose.vps.yml:
#   ${DOCKERHUB_USERNAME}/job-portal-<name>:latest
#
# Env:
#   DOCKERHUB_USERNAME  (required)
#   DOCKERHUB_TOKEN     (required) — Hub access token, not account password
#   IMAGE_TAG           (optional, default latest)
#   SHORT_SHA           (optional, second tag)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

MVNW="./services/job-user-service/mvnw"
chmod +x "$MVNW"

: "${DOCKERHUB_USERNAME:?Set DOCKERHUB_USERNAME}"
: "${DOCKERHUB_TOKEN:?Set DOCKERHUB_TOKEN}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
SHORT_SHA="${SHORT_SHA:-}"

# module path | Hub name suffix (after job-portal-)
MODULES=(
  "cloud/eureka-server|eureka-server"
  "cloud/config-server|config-server"
  "cloud/api-gateway|api-gateway"
  "services/job-user-service|job-user-service"
  "services/job-portal-company-service|job-portal-company-service"
  "services/job-portal-job-service|job-portal-job-service"
  "services/job-portal-resume-service|job-portal-resume-service"
  "services/job-portal-application-service|job-portal-application-service"
  "services/job-portal-preferences|job-portal-preferences-service"
  "services/job-portal-ai-service|job-portal-ai-service"
  "services/job-portal-notification-service|job-portal-notification-service"
)

TAGS="$IMAGE_TAG"
if [[ -n "$SHORT_SHA" ]]; then
  TAGS="${IMAGE_TAG},${SHORT_SHA}"
fi

echo "Pushing as ${DOCKERHUB_USERNAME}/job-portal-*:${TAGS}"

for entry in "${MODULES[@]}"; do
  module="${entry%%|*}"
  name="${entry##*|}"
  image="${DOCKERHUB_USERNAME}/job-portal-${name}"

  echo ""
  echo "=== Jib build → ${image} (tags: ${TAGS}) ==="

  "$MVNW" -B -f pom.xml \
    -pl "$module" -am package jib:build \
    -DskipTests \
    "-Djib.to.image=${image}" \
    "-Djib.to.tags=${TAGS}" \
    "-Djib.to.auth.username=${DOCKERHUB_USERNAME}" \
    "-Djib.to.auth.password=${DOCKERHUB_TOKEN}"
done

echo ""
echo "Done. Example pull: docker pull ${DOCKERHUB_USERNAME}/job-portal-eureka-server:${IMAGE_TAG}"
