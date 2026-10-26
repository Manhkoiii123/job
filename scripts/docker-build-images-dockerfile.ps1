# Build all Job Portal images using Dockerfile (no Jib)
# Requires Docker Desktop running.
# Usage (from repo root):  .\scripts\docker-build-images-dockerfile.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "Building images via docker/Dockerfile.jvm (compose override)..." -ForegroundColor Cyan
docker compose -f docker-compose.yml -f docker-compose.dockerfile.yml build
if ($LASTEXITCODE -ne 0) { throw "docker compose build failed" }

Write-Host "`nDone. Images:" -ForegroundColor Green
docker images "job-portal/*"
