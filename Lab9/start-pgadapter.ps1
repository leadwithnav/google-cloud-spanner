param(
    [string]$ProjectId = "instructor-04",
    [string]$InstanceId = "test-instance1",
    [string]$DatabaseId = "spanner-bank-db",
    [int]$Port = 5432
)

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Starting Cloud Spanner PGAdapter on port ${Port}..." -ForegroundColor Cyan
Write-Host "Project:  ${ProjectId}" -ForegroundColor Yellow
Write-Host "Instance: ${InstanceId}" -ForegroundColor Yellow
Write-Host "Database: ${DatabaseId}" -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor Cyan

docker stop spanner-pgadapter 2>$null
docker rm spanner-pgadapter 2>$null

$gcloudPath = "$env:APPDATA\gcloud"

docker run -d `
  --name spanner-pgadapter `
  -p "${Port}:5432" `
  -v "${gcloudPath}:/root/.config/gcloud:ro" `
  gcr.io/cloud-spanner-pg-adapter/pgadapter:latest `
  -p $ProjectId `
  -i $InstanceId `
  -d $DatabaseId `
  -x

Write-Host "PGAdapter proxy is running on localhost:${Port}. Ready for Spring Boot connections!" -ForegroundColor Green
