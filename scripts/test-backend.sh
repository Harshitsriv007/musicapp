#!/usr/bin/env bash
# Runs backend unit tests (+ acceptance tests with "all") inside Docker – no local Java needed.
set -euo pipefail
cd "$(dirname "$0")/../backend"
GOAL=test
[[ "${1:-}" == "all" ]] && GOAL=verify
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  -v "$PWD":/app -v musicapp-m2:/root/.m2 -w /app \
  maven:3.9.9-eclipse-temurin-21 mvn -B "$GOAL"
