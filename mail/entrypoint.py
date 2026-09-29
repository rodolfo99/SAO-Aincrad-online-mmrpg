#!/usr/bin/env python3
"""Start the bundled outbound MTA with an IP literal, never a public relay."""
import ipaddress
import os
import subprocess
import sys


def settings(address, subnet):
    ip = ipaddress.IPv4Address(address)
    network = ipaddress.IPv4Network(subnet, strict=True)
    if ip.is_unspecified or ip.is_multicast or ip == ipaddress.IPv4Address('255.255.255.255'):
        raise ValueError('MAIL_PUBLIC_IP debe ser una dirección unicast')
    if not network.is_private or network.prefixlen < 24:
        raise ValueError('MAIL_NETWORK_SUBNET debe ser una red privada IPv4 /24 o menor')
    return [f'myorigin = [{ip}]', f'smtp_helo_name = [{ip}]', f'mynetworks = 127.0.0.0/8, {network}']


if __name__ == '__main__':
    try:
        config = settings(os.environ.get('MAIL_PUBLIC_IP', ''), os.environ.get('MAIL_NETWORK_SUBNET', '172.30.42.0/28'))
        if os.environ.get('MAIL_ALLOW_HOST') == 'true':
            # Optional host-Java mode publishes SMTP on loopback only. Trust its bridge gateway.
            with open('/proc/net/route') as routes:
                gateway = next(line.split()[2] for line in routes if len(line.split()) > 3 and line.split()[1] == '00000000')
            config[-1] += ', ' + str(ipaddress.IPv4Address(bytes.fromhex(gateway)[::-1])) + '/32'
    except ValueError as error:
        sys.exit(f'Correo no iniciado: {error}. Ejecuta scripts/correo.sh configurar.')
    subprocess.run(['postconf', '-e', *config], check=True)
    subprocess.run(['postfix', 'check'], check=True)
    print('Correo directo por IP iniciado. Cola persistente; salida SMTP 25; sin servicio SMTP externo.', flush=True)
    os.execvp('postfix', ['postfix', 'start-fg'])
