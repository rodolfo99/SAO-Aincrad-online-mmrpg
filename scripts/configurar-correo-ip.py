#!/usr/bin/env python3
"""Configure the built-in IP-based mail utility without executing .env as shell code."""
import argparse
import ipaddress
import os
from pathlib import Path
import re
import shutil
import socket
import sys
import time
from urllib.parse import urlsplit
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]


def read_env(path):
    values = {}
    if path.exists():
        for line in path.read_text().splitlines():
            match = re.fullmatch(r'([A-Z][A-Z0-9_]*)=(.*)', line.strip())
            if match:
                value = match[2].strip()
                if len(value) >= 2 and value[0] == value[-1] and value[0] in ('"', "'"):
                    value = value[1:-1]
                values[match[1]] = value
    return values


def configure(root, address, url=None):
    ip = ipaddress.IPv4Address(address)
    if ip.is_unspecified or ip.is_multicast or ip == ipaddress.IPv4Address('255.255.255.255'):
        raise ValueError('La IP debe ser una dirección unicast de esta máquina')
    target = root / '.env'
    source = target if target.exists() else root / '.env.example'
    values = read_env(source)
    previous_url = values.get('APP_PUBLIC_URL', '')
    if not url:
        url = previous_url if previous_url and 'localhost' not in previous_url and '127.0.0.1' not in previous_url else f"http://{ip}:{values.get('WEB_PORT') or '8080'}"
    parsed = urlsplit(url)
    if parsed.scheme not in ('https', 'http') or parsed.hostname not in (str(ip), 'localhost', '127.0.0.1') or parsed.username or parsed.password or parsed.path not in ('', '/') or parsed.query or parsed.fragment:
        raise ValueError('La URL debe ser http(s)://IP:PUERTO del juego, sin rutas ni credenciales')
    url = url.rstrip('/')
    origins = [x for x in values.get('ALLOWED_ORIGINS', '').split(',') if x]
    if url not in origins:
        origins.append(url)
    changes = {'MAIL_ENABLED': 'true', 'MAIL_PUBLIC_IP': str(ip), 'MAIL_FROM': f'noreply@[{ip}]',
               'APP_PUBLIC_URL': url, 'ALLOWED_ORIGINS': ','.join(origins),
               'PLAYER_COOKIE_SECURE': 'true' if parsed.scheme == 'https' else 'false'}
    original = source.read_text() if source.exists() else ''
    lines = original.splitlines()
    remaining = dict(changes)
    for i, line in enumerate(lines):
        key = line.split('=', 1)[0].strip()
        if key in changes:
            lines[i] = f'{key}={changes[key]}'
            remaining.pop(key, None)
    lines.extend(f'{key}={value}' for key, value in remaining.items())
    if target.exists():
        backups = root / 'backups'
        backups.mkdir(exist_ok=True)
        backup = backups / f'env-before-mail-{time.time_ns()}.bak'
        shutil.copyfile(target, backup)
        backup.chmod(0o600)
    temporary = root / '.env.mail.partial'
    temporary.write_text('\n'.join(lines) + '\n')
    temporary.chmod(0o600)
    os.replace(temporary, target)
    return ip, url


def detect_ip():
    # An empty HTTPS request discovers the NAT address. No email or credentials are sent.
    try:
        with urlopen('https://api.ipify.org', timeout=5) as response:
            return str(ipaddress.IPv4Address(response.read(64).decode().strip()))
    except Exception:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.connect(('1.1.1.1', 80))
            return sock.getsockname()[0]


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ip', help='IPv4 de salida; omitir para detectar automáticamente')
    parser.add_argument('--url', help='URL del juego por IP; se conserva la URL existente si está configurada')
    args = parser.parse_args()
    try:
        ip, url = configure(ROOT, args.ip or os.environ.get('MAIL_PUBLIC_IP') or read_env(ROOT / '.env').get('MAIL_PUBLIC_IP') or detect_ip(), args.url)
    except (ValueError, OSError) as error:
        sys.exit(f'No se configuró el correo: {error}. Puedes indicar --ip y --url.')
    print(f'Remitente: noreply@[{ip}]\nEnlaces del juego: {url}\nConfiguración guardada; no se requiere dominio ni cuenta SMTP externa.')
    if not ip.is_global:
        print('La IP es privada/local. Para destinatarios en Internet, indica la IPv4 pública de salida de esta máquina.')
    if url.startswith('http:'):
        print('La URL usa HTTP sin cifrado. Utiliza HTTPS por IP al publicar el juego.')
