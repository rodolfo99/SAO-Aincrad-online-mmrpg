#!/usr/bin/env python3
"""Submit a test message to the bundled Postfix queue; never claim final delivery."""
import argparse
from email.message import EmailMessage
from email.policy import SMTP
from email.utils import formatdate, make_msgid, parseaddr
import importlib.util
import ipaddress
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('mail_config', root / 'scripts/configurar-correo-ip.py')
config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(config)
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('recipient')
args = parser.parse_args()
recipient = args.recipient.strip()
if len(recipient) > 254 or any(c in recipient for c in '\r\n,;') or parseaddr(recipient)[1] != recipient or '@' not in recipient:
    sys.exit('Introduce un único correo válido.')
values = config.read_env(root / '.env')
try:
    ip = ipaddress.IPv4Address(values['MAIL_PUBLIC_IP'])
except (KeyError, ValueError):
    sys.exit('Primero ejecuta ./scripts/correo.sh configurar.')
message = EmailMessage(policy=SMTP)
message['From'] = f'noreply@[{ip}]'
message['To'] = recipient
message['Subject'] = 'Aincrad — Prueba de correo directo por IP'
message['Date'] = formatdate(localtime=False)
message['Message-ID'] = make_msgid(domain=f'[{ip}]')
message.set_content('Este mensaje comprueba el envío de correo del servidor Aincrad.\n\nSe ha enviado mediante la utilidad Postfix integrada, directamente desde la IP de la máquina, sin un servicio SMTP externo ni un dominio propio.\n\nNo contiene contraseñas ni enlaces de recuperación. Revisa si llegó a la bandeja de entrada o a correo no deseado.\n')
result = subprocess.run(['docker', 'compose', 'exec', '-T', 'mail', 'sendmail', '-i', '-t', '-f', f'noreply@[{ip}]'], cwd=root, input=message.as_bytes())
if result.returncode:
    sys.exit('No se aceptó la prueba. Revisa ./scripts/correo.sh registro.')
print('Prueba aceptada en la cola local. Esto no confirma la entrega final. Consulta cola/registro y el buzón destinatario.')
