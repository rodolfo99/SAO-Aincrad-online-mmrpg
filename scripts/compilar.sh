#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
for tool in java javac mvn node npm; do command -v "$tool" >/dev/null || { echo "Falta $tool. Consulta README.md" >&2; exit 1; }; done
node -e 'const [a,b]=process.versions.node.split(".").map(Number);if(!((a===20&&b>=19)||(a===22&&b>=12)||a===24)){console.error("Usa Node 22.22.0 o una versión compatible indicada en README");process.exit(1)}'
npm --prefix client ci --no-audit --no-fund
npm --prefix client test
npm --prefix client run build
mvn -B -f server/pom.xml verify
mkdir -p release
cp server/target/aincrad-server-0.2.0.jar release/aincrad-server-0.2.0.jar
printf '%s\n' 'Compilación completada. Ejecuta ./scripts/iniciar.sh'
