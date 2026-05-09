#!/usr/bin/env bash
set -euo pipefail

# Navigate to project root regardless of where script is called from
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# Start all services using local docker-compose config
# --build forces Docker to rebuild the api image using Dockerfile.local
# Gradle dependency caching inside Docker means subsequent builds are faster
docker compose -f docker-compose.local.yml up --build

echo "API is running at http://localhost:8080"