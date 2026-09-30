#!/usr/bin/env bash
set -euo pipefail
SAO_PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
command -v mvn >/dev/null || { echo 'Instala Maven 3.9.9 y un JDK 17 o 21.' >&2; exit 1; }
exec mvn -B -f "$SAO_PROJECT_DIR/client-java/pom.xml" verify "$@"
