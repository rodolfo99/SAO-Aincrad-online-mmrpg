#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
command -v java >/dev/null || { echo 'Instala Java 17 o 21.' >&2; exit 1; }
AINCRAD_JAR='release/aincrad-server-0.2.0.jar'
if [[ ! -f "$AINCRAD_JAR" || ! -f client/dist/aincrad/browser/index.html ]]; then ./scripts/compilar.sh; fi
mkdir -p client/dist/aincrad/browser/assets
cp -a client/public/assets/. client/dist/aincrad/browser/assets/
export DATA_DIR="${DATA_DIR:-$ROOT_DIR/data}" WORLD_FILE="${WORLD_FILE:-$ROOT_DIR/world/world.json}"
if [[ ! -f "$DATA_DIR/admin.json" && -z "${ROOT_PASSWORD:-}" ]]; then
  [[ -t 0 ]] || { echo 'Primer inicio: configura ROOT_PASSWORD (mínimo 12 caracteres).' >&2; exit 1; }
  read -r -s -p 'Define la contraseña de root (12–128 caracteres): ' ROOT_PASSWORD; echo
  read -r -s -p 'Repite la contraseña: ' AINCRAD_CONFIRM; echo
  [[ "$ROOT_PASSWORD" == "$AINCRAD_CONFIRM" && ${#ROOT_PASSWORD} -ge 12 && ${#ROOT_PASSWORD} -le 128 ]] || { echo 'Las contraseñas no coinciden o su longitud no es válida.' >&2; exit 1; }
  unset AINCRAD_CONFIRM
fi
export ROOT_PASSWORD="${ROOT_PASSWORD:-}"
printf '%s\n' "Juego: http://localhost:${PORT:-8080}" "Admin: http://localhost:${PORT:-8080}/admin · usuario root" 'Ctrl+C guarda y cierra el servidor.'
exec java -Dfile.encoding=UTF-8 -jar "$AINCRAD_JAR"
