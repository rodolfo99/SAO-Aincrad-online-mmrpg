#!/usr/bin/env bash
set -euo pipefail
SAO_CLIENT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
command -v java >/dev/null || { echo 'Instala Java 17 o 21.' >&2; exit 1; }
if [[ -f "$SAO_CLIENT_DIR/aincrad-client-java-0.1.0.jar" ]]; then
  SAO_CLIENT_CP="$SAO_CLIENT_DIR/aincrad-client-java-0.1.0.jar:$SAO_CLIENT_DIR/lib/*"
elif [[ -f "$SAO_CLIENT_DIR/target/aincrad-client-java-0.1.0.jar" ]]; then
  SAO_CLIENT_CP="$SAO_CLIENT_DIR/target/aincrad-client-java-0.1.0.jar:$SAO_CLIENT_DIR/target/lib/*"
else
  echo 'Primero ejecuta: mvn -f client-java/pom.xml verify (desde la raíz del repositorio).' >&2
  exit 1
fi
if [[ ${1:-} == http://* || ${1:-} == https://* ]]; then
  SAO_CLIENT_URL="$1"; shift
  set -- "--server=$SAO_CLIENT_URL" "$@"
fi
exec java -Xmx1536m -Dfile.encoding=UTF-8 -cp "$SAO_CLIENT_CP" dev.aincrad.client.Launcher "$@"
