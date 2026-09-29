#!/usr/bin/env python3
"""Create a portable source + runnable release, omitting runtime data and caches."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
target = root.parent / 'aincrad-seed-v0.2.0.zip'
blocked = {'node_modules', '.angular', 'target', '.tools', 'data', 'logs', '.git', '__pycache__', 'backups'}

def include(path):
    rel = path.relative_to(root)
    if any(part in blocked for part in rel.parts):
        return False
    if path.name in {'.env', 'SHA256SUMS.txt'} or path.name.startswith('failure-') or path.suffix in {'.partial', '.log'}:
        return False
    if rel.as_posix().startswith('client/dist/aincrad/browser/assets/'):
        return False  # iniciar.sh restores these from public/assets without downloading.
    return path.is_file() and not path.is_symlink()

files = sorted(p for p in root.rglob('*') if include(p))
required = ['release/aincrad-server-0.2.0.jar', 'client/dist/aincrad/browser/index.html', 'world/world.json', 'client/package-lock.json', 'server/pom.xml', 'docs/VALIDACION.md']
names = {p.relative_to(root).as_posix() for p in files}
for name in required:
    if name not in names:
        raise SystemExit(f'Falta {name}; compila y valida antes de empaquetar.')
checksum = ''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.relative_to(root).as_posix()}\n' for p in files)
(root / 'SHA256SUMS.txt').write_text(checksum, encoding='utf-8')
with zipfile.ZipFile(target, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in files + [root / 'SHA256SUMS.txt']:
        archive.write(path, 'aincrad-seed/' + path.relative_to(root).as_posix())
with zipfile.ZipFile(target) as archive:
    assert archive.testzip() is None
print(f'{target}\n{target.stat().st_size} bytes\nSHA256 {hashlib.sha256(target.read_bytes()).hexdigest()}')
