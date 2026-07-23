#!/usr/bin/env bash
# =========================================================================
# Cloud Spanner PGAdapter Sidecar Docker Launcher (Linux/macOS)
# =========================================================================

PROJECT_ID=${1:-"instructor-04"}
INSTANCE_ID=${2:-"test-instance1"}
DATABASE_ID=${3:-"spanner-bank-db"}
PORT=${4:-5432}

echo "================================================================="
echo "Starting Cloud Spanner PGAdapter on port ${PORT}..."
echo "Project:  ${PROJECT_ID}"
echo "Instance: ${INSTANCE_ID}"
echo "Database: ${DATABASE_ID}"
echo "================================================================="

docker stop spanner-pgadapter 2>/dev/null || true
docker rm spanner-pgadapter 2>/dev/null || true

docker run -d \
  --name spanner-pgadapter \
  -p ${PORT}:5432 \
  -v "${HOME}/.config/gcloud:/root/.config/gcloud:ro" \
  gcr.io/cloud-spanner-pg-adapter/pgadapter:latest \
  -p ${PROJECT_ID} \
  -i ${INSTANCE_ID} \
  -d ${DATABASE_ID} \
  -x

echo "PGAdapter proxy is running on localhost:${PORT}. Ready for Spring Boot connections!"
