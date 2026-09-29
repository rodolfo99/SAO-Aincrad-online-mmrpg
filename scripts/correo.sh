#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
ACTION="${1:-ayuda}"
if [[ $# -gt 0 ]]; then shift; fi
case "$ACTION" in
  configurar) exec python3 scripts/configurar-correo-ip.py "$@" ;;
  iniciar)
    python3 scripts/configurar-correo-ip.py "$@"
    docker compose --profile mail up -d --build mail game
    ;;
  iniciar-local)
    python3 scripts/configurar-correo-ip.py "$@"
    docker compose -f compose.yaml -f compose.mail-local.yaml --profile mail up -d --build mail
    printf '%s\n' 'Utilidad disponible sólo en 127.0.0.1:2525. Inicia Java con ./scripts/iniciar-con-correo.sh.'
    ;;
  cola) exec docker compose exec -T mail postqueue -p ;;
  registro) exec docker compose logs --tail=80 mail ;;
  reintentar) exec docker compose exec -T mail postqueue -f ;;
  prueba) exec python3 scripts/correo-prueba.py "$@" ;;
  *) printf '%s\n' 'Uso: ./scripts/correo.sh configurar|iniciar|iniciar-local [--ip IPv4] [--url URL]' '     ./scripts/correo.sh prueba destinatario@correo.com' '     ./scripts/correo.sh cola|registro|reintentar' ;;
esac
