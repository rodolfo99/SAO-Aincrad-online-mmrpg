#!/usr/bin/env bash
set -u
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
printf '%s\n' '=== Entorno ==='
java -version 2>&1 || true
node --version || true
mvn --version || true
docker compose version || true
printf '%s\n' '=== Archivos requeridos ==='
for path in world/world.json server/pom.xml client/package-lock.json client/public/assets/lyra.png release/aincrad-server-0.2.0.jar client/dist/aincrad/browser/index.html; do if [[ -f "$path" ]];then printf 'OK %s\n' "$path";else printf 'FALTA %s\n' "$path";fi;done
printf '%s\n' '=== Servidor ==='
curl --fail --silent --show-error --max-time 4 "http://127.0.0.1:${PORT:-8080}/api/health" || true
printf '\n'
