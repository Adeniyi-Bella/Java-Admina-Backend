#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

./gradlew clean assemble -x test
docker compose up --build

echo "API is running at http://localhost:8080"
