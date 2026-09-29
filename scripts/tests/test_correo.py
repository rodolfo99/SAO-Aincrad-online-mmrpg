import importlib.util
from pathlib import Path
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[2]
def module(name,path):
    spec=importlib.util.spec_from_file_location(name,path);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
config=module('config',ROOT/'scripts/configurar-correo-ip.py')
entry=module('entry',ROOT/'mail/entrypoint.py')
class MailConfigurationTest(unittest.TestCase):
    def test_keeps_credentials_and_origins_adds_ip_literal_and_private_backup(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);original='ROOT_PASSWORD=Private-not-a-shell-command\nALLOWED_ORIGINS=http://localhost:8080\nWEB_PORT=9090\n'
            (root/'.env').write_text(original);config.configure(root,'203.0.113.8')
            values=config.read_env(root/'.env');self.assertEqual(values['ROOT_PASSWORD'],'Private-not-a-shell-command');self.assertEqual(values['MAIL_FROM'],'noreply@[203.0.113.8]');self.assertEqual(values['APP_PUBLIC_URL'],'http://203.0.113.8:9090');self.assertIn('http://localhost:8080',values['ALLOWED_ORIGINS']);self.assertEqual((root/'.env').stat().st_mode&0o777,0o600);self.assertEqual(next((root/'backups').iterdir()).read_text(),original)
    def test_invalid_addresses_and_urls_never_change_existing_file(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);(root/'.env').write_text('ROOT_PASSWORD=unchanged\n')
            for address,url in [('not-ip',None),('0.0.0.0',None),('224.0.0.1',None),('203.0.113.8','http://evil.invalid'),('203.0.113.8','http://user:pass@203.0.113.8'),('203.0.113.8','http://203.0.113.8/?token=abc')]:
                with self.assertRaises(ValueError):config.configure(root,address,url)
                self.assertEqual((root/'.env').read_text(),'ROOT_PASSWORD=unchanged\n')
    def test_mta_restricts_relay_to_private_subnet(self):
        values=entry.settings('203.0.113.8','172.30.42.0/28');self.assertIn('smtp_helo_name = [203.0.113.8]',values);self.assertEqual(values[-1],'mynetworks = 127.0.0.0/8, 172.30.42.0/28')
        for network in ('0.0.0.0/0','8.8.8.0/24','10.0.0.0/8','172.30.42.1/28'):
            with self.assertRaises(ValueError):entry.settings('203.0.113.8',network)
    def test_https_origin_preserves_secure_cookie(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);config.configure(root,'203.0.113.8','https://203.0.113.8');self.assertEqual(config.read_env(root/'.env')['PLAYER_COOKIE_SECURE'],'true')
if __name__=='__main__':unittest.main()
