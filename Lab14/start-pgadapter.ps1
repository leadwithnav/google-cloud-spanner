# =========================================================================
# Start Cloud Spanner PGAdapter Proxy locally via Docker (PowerShell)
# =========================================================================

param (
    [string]$ProjectId = "my-gcp-project",
    [string]$InstanceId = "test-instance",
    [string]$DatabaseId = "spanner-bank-db",
    [int]$Port = 5432
)

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Starting Cloud Spanner PGAdapter on port $Port..." -ForegroundColor Green
Write-Host "Project:  $ProjectId" -ForegroundColor Yellow
Write-Host "Instance: $InstanceId" -ForegroundColor Yellow
Write-Host "Database: $DatabaseId" -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Cyan

docker run -d `
  --name spanner-pgadapter `
  -p ${Port}:5432 `
  -v $env:APPDATA\gcloud:/root/.config/gcloud `
  gcr.io/cloud-spanner-pg-adapter/pgadapter:latest `
  -p $ProjectId `
  -i $InstanceId `
  -d $DatabaseId `
  -x

Write-Host "PGAdapter proxy is running on localhost:$Port. Ready for Spring Boot connections!" -ForegroundColor Green
