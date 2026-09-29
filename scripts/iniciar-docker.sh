#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
docker compose version >/dev/null
if [[ -z "${ROOT_PASSWORD:-}" ]]; then
  read -r -s -p 'Contraseña root para inicializar un volumen nuevo (vacío si ya está inicializado o configuraste .env): ' AINCRAD_PASSWORD; echo
  if [[ -n "$AINCRAD_PASSWORD" ]]; then
    [[ ${#AINCRAD_PASSWORD} -ge 12 && ${#AINCRAD_PASSWORD} -le 128 ]] || { echo 'Se requieren 12–128 caracteres.' >&2;exit 1; }
    read -r -s -p 'Confirma la contraseña: ' AINCRAD_CONFIRM; echo
    [[ "$AINCRAD_PASSWORD" == "$AINCRAD_CONFIRM" ]] || { echo 'No coinciden.' >&2;exit 1; }
    export ROOT_PASSWORD="$AINCRAD_PASSWORD"
    unset AINCRAD_PASSWORD AINCRAD_CONFIRM
  fi
fi
docker compose "$@" up --build -d
printf '%s\n' 'Consulta docker compose logs -f game para confirmar el arranque.'
