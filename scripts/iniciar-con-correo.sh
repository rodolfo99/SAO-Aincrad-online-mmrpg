#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
python3 - <<'PY'
import importlib.util,os
from pathlib import Path
root=Path.cwd()
spec=importlib.util.spec_from_file_location('mail_config',root/'scripts/configurar-correo-ip.py')
config=importlib.util.module_from_spec(spec);spec.loader.exec_module(config)
values=config.read_env(root/'.env')
if values.get('MAIL_ENABLED')!='true':
    raise SystemExit('Primero ejecuta ./scripts/correo.sh iniciar-local.')
for key in ('ROOT_PASSWORD','MAIL_ENABLED','MAIL_PUBLIC_IP','MAIL_FROM','APP_PUBLIC_URL','ALLOWED_ORIGINS','PLAYER_COOKIE_SECURE','PLAYER_REGISTRATION_ENABLED','PLAYER_SESSION_HOURS','PASSWORD_RESET_MINUTES'):
    if values.get(key): os.environ.setdefault(key,values[key])
os.environ['MAIL_HOST']='127.0.0.1';os.environ['MAIL_PORT']='2525'
os.execvpe('bash',['bash','scripts/iniciar.sh'],os.environ)
PY
