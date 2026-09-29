#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
[[ -d data ]] || { echo 'No hay datos locales que respaldar.' >&2; exit 1; }
mkdir -p backups
BACKUP_FILE="backups/aincrad-$(date +%Y%m%d-%H%M%S).tar.gz"
printf '%s\n' 'Detén primero el servidor para una copia coherente. Para Docker consulta docs/OPERACION.md.'
read -r -p '¿El servidor está detenido? escribe SI: ' ANSWER
[[ "$ANSWER" == SI ]] || exit 1
umask 077
tar -czf "$BACKUP_FILE.partial" data world
mv "$BACKUP_FILE.partial" "$BACKUP_FILE"
printf 'Respaldo privado creado: %s\n' "$BACKUP_FILE"
