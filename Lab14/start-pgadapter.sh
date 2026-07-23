#!/bin/bash
# =========================================================================
# Start Cloud Spanner PGAdapter Proxy locally via Docker
# =========================================================================

PROJECT_ID=${1:-"my-gcp-project"}
INSTANCE_ID=${2:-"test-instance1"}
DATABASE_ID=${3:-"spanner-bank-db"}
PORT=${4:-5432}

echo "================================================================="
echo "Starting Cloud Spanner PGAdapter on port ${PORT}..."
echo "Project:  ${PROJECT_ID}"
echo "Instance: ${INSTANCE_ID}"
echo "Database: ${DATABASE_ID}"
echo "================================================================="

# Execute PGAdapter Container
docker run -d \
  --name spanner-pgadapter \
  -p ${PORT}:5432 \
  -v ~/.config/gcloud:/root/.config/gcloud \
  gcr.io/cloud-spanner-pg-adapter/pgadapter:latest \
  -p ${PROJECT_ID} \
  -i ${INSTANCE_ID} \
  -d ${DATABASE_ID} \
  -x

echo "PGAdapter proxy is running on localhost:${PORT}. Ready for Spring Boot connections!"
