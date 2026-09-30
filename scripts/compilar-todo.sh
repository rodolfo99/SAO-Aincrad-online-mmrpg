#!/usr/bin/env bash
set -euo pipefail
SAO_PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
bash "$SAO_PROJECT_DIR/scripts/compilar.sh"
exec bash "$SAO_PROJECT_DIR/scripts/compilar-cliente-java.sh" "$@"
