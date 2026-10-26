# Build all Job Portal Docker images via Google Jib (requires Docker Desktop running)
# Usage (from repo root):  .\scripts\docker-build-images.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$Mvnw = Join-Path $Root "services\job-user-service\mvnw.cmd"
if (-not (Test-Path $Mvnw)) {
    throw "mvnw.cmd not found at $Mvnw"
}

$Modules = @(
    "cloud/eureka-server",
    "cloud/config-server",
    "cloud/api-gateway",
    "services/job-user-service",
    "services/job-portal-company-service",
    "services/job-portal-job-service",
    "services/job-portal-resume-service",
    "services/job-portal-application-service",
    "services/job-portal-preferences",
    "services/job-portal-ai-service",
    "services/job-portal-notification-service"
)

Write-Host "Building common-lib first..." -ForegroundColor Cyan
& $Mvnw -f pom.xml -pl common-lib -am package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "common-lib build failed" }

foreach ($module in $Modules) {
    Write-Host "`n=== Jib dockerBuild: $module ===" -ForegroundColor Cyan
    & $Mvnw -f pom.xml -pl $module -am package jib:dockerBuild -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Jib failed for $module" }
}

Write-Host "`nDone. Images:" -ForegroundColor Green
docker images "job-portal/*"
