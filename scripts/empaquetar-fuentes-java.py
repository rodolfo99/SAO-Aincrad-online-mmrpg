#!/usr/bin/env python3
"""Empaqueta fuentes Java, recursos compartidos, documentación y compilación."""
from pathlib import Path
import hashlib
import zipfile

root = Path(__file__).resolve().parents[1]
target = root.parent / "SAO-Java-Desktop-0.1.0-fuentes-completo.zip"
prefix = "SAO-Java-Desktop-0.1.0-fuentes"
blocked = {"node_modules", ".angular", "target", ".tools", "data", "logs", ".git", "__pycache__", "backups"}


def include(path):
    rel = path.relative_to(root)
    if any(part in blocked for part in rel.parts):
        return False
    if path.name in {".env", "SHA256SUMS.txt"} or path.name.startswith("failure-") or path.suffix in {".partial", ".log"}:
        return False
    if rel.as_posix().startswith("client/dist/aincrad/browser/assets/"):
        return False
    return path.is_file() and not path.is_symlink()


files = sorted(p for p in root.rglob("*") if include(p))
names = {p.relative_to(root).as_posix() for p in files}
required = [
    "client-java/pom.xml",
    "client-java/src/main/java/dev/aincrad/client/Launcher.java",
    "client-java/src/test/java/dev/aincrad/client/ServerContractTest.java",
    "client-java/src/main/resources/art/models.json.gz",
    "client-java/src/main/resources/art/manifest.json",
    "client-java/src/assembly/desktop.xml",
    "client-java/tools/export-models.mjs",
    "client-java/README.md",
    "docs/CLIENTE-JAVA.md",
    "docs/PAQUETE-FUENTES-JAVA.md",
    "docs/VALIDACION-PAQUETE-JAVA.md",
    "README-FUENTES-JAVA.md",
    "scripts/compilar-cliente-java.sh",
    "scripts/iniciar-cliente-java.sh",
    "scripts/compilar-todo.sh",
    "release/aincrad-server-0.2.0.jar",
    "world/world.json",
    "client/public/assets/ASSETS.md",
    "client/package-lock.json",
    "server/pom.xml",
]
missing = [name for name in required if name not in names]
if missing:
    raise SystemExit("Faltan archivos necesarios: " + ", ".join(missing))

checksums = root / "SHA256SUMS.txt"
checksums.write_text("".join(f"{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.relative_to(root).as_posix()}\n" for p in files), encoding="utf-8")
with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for path in files + [checksums]:
        archive.write(path, f"{prefix}/{path.relative_to(root).as_posix()}")
with zipfile.ZipFile(target) as archive:
    bad = archive.testzip()
    if bad:
        raise SystemExit(f"Fallo de integridad ZIP: {bad}")
print(target)
print(f"Archivos: {len(files) + 1}")
print(f"Tamaño: {target.stat().st_size} bytes")
print(f"SHA256: {hashlib.sha256(target.read_bytes()).hexdigest()}")
