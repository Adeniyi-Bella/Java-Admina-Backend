#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo "Stopping compose stack and removing project resources..."
docker compose down --volumes --remove-orphans || true

echo "Removing all Docker containers/images/networks/volumes from this machine..."
docker rm -f $(docker ps -aq) 2>/dev/null || true
docker system prune -a --volumes -f

echo "Docker has been fully cleaned."
