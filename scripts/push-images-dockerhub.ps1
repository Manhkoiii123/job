# Push local job-portal/* images to Docker Hub
# Prereq: docker login ; images already built (Jib or Dockerfile)
# Usage:
#   $env:DOCKERHUB_USER = "youruser"
#   .\scripts\push-images-dockerhub.ps1

$ErrorActionPreference = "Stop"
if (-not $env:DOCKERHUB_USER) {
    throw "Set DOCKERHUB_USER first, e.g. `$env:DOCKERHUB_USER='myuser'"
}

$User = $env:DOCKERHUB_USER
$Images = @(
    "eureka-server",
    "config-server",
    "api-gateway",
    "job-user-service",
    "job-portal-company-service",
    "job-portal-job-service",
    "job-portal-resume-service",
    "job-portal-application-service",
    "job-portal-preferences-service",
    "job-portal-ai-service",
    "job-portal-notification-service"
)

foreach ($name in $Images) {
    $local = "job-portal/${name}:latest"
    $remote = "${User}/job-portal-${name}:latest"
    Write-Host "Tag & push $local -> $remote" -ForegroundColor Cyan
    docker tag $local $remote
    if ($LASTEXITCODE -ne 0) { throw "tag failed: $local" }
    docker push $remote
    if ($LASTEXITCODE -ne 0) { throw "push failed: $remote" }
}

Write-Host "Done. On VPS pull with: docker pull $User/job-portal-<service>:latest" -ForegroundColor Green
