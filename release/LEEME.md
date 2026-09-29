# Ejecutables de esta entrega

`aincrad-server-0.2.0.jar` es el resultado de Maven `verify` con Java 17.0.20. Las fuentes, pruebas y `pom.xml` están en `server/`.

El cliente construido se encuentra en `client/dist/aincrad/browser/`. Para reducir duplicación, el ZIP conserva las ilustraciones únicamente en `client/public/assets/`: `scripts/iniciar.sh` las copia a la carpeta del cliente al arrancar. No necesita descargarlas.

Ejecuta `./scripts/iniciar.sh` desde cualquier ubicación; el script resuelve la raíz del proyecto. En el primer inicio se solicita la contraseña de root. La contraseña y los datos de una partida nunca forman parte del paquete.

Para reconstruir: `./scripts/compilar.sh`. Para volver a empaquetar después de validar: `python3 scripts/empaquetar.py`. `SHA256SUMS.txt` permite comprobar los archivos incluidos con `sha256sum -c SHA256SUMS.txt` desde la raíz extraída.
