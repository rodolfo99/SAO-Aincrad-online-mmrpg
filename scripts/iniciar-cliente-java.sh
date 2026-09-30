#!/usr/bin/env bash
set -euo pipefail
SAO_PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec bash "$SAO_PROJECT_DIR/client-java/iniciar-cliente.sh" "$@"
