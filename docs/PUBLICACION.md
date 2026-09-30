# Publicación, actualización y empaquetado

## Estado comprobado el 29 de septiembre de 2026

El proyecto está publicado en [`rodolfo99/SAO-Aincrad-online-mmrpg`](https://github.com/rodolfo99/SAO-Aincrad-online-mmrpg), rama `main`. La subida del usuario quedó en el commit [`944b3ab`](https://github.com/rodolfo99/SAO-Aincrad-online-mmrpg/commit/944b3ab778733de18708a87381dcfe5952e0c806), con el mensaje `Publicar proyecto Aincrad v0.2.0 con fuentes y compilados`.

Se compararon los 255 archivos de esa publicación con el ZIP `aincrad-seed-v0.2.0.zip`: sus hashes de objetos Git coinciden. La actualización documental posterior añade esta guía y [ARRANQUE-Y-PUERTOS.md](ARRANQUE-Y-PUERTOS.md), actualiza los documentos relacionados y regenera el manifiesto de integridad. La versión del juego permanece en **v0.2.0**.

| Contenido publicado | Ubicación |
|---|---|
| Fuentes y pruebas del servidor | `server/` |
| Fuentes y pruebas de Angular/Three.js | `client/src/`, `client/tests/` |
| Ilustraciones originales | `client/public/assets/` |
| Servidor ejecutable de esta versión | `release/aincrad-server-0.2.0.jar` |
| Cliente compilado | `client/dist/aincrad/browser/` |
| Mundo inicial y ampliación | `world/` |
| Operación, compilación y empaquetado | `scripts/`, `Dockerfile`, `compose*.yaml` |
| Guías, resultados históricos y capturas | `docs/` |

El JAR 0.1.0 permanece como artefacto anterior; `scripts/iniciar.sh` utiliza 0.2.0. No arranques la versión anterior contra los datos de cuentas de la entrega actual.

En la revisión de publicación no había una **GitHub Release** creada. El ZIP original de entrega, **Code → Download ZIP** de `main` y un futuro adjunto de Releases son distribuciones distintas. La publicación de fuentes no implica que exista un tag o Release v0.2.0. El ZIP original conserva la documentación de su generación; clona `main` para obtener estas guías posteriores.

## Descargar y ejecutar

```bash
git clone https://github.com/rodolfo99/SAO-Aincrad-online-mmrpg.git
cd SAO-Aincrad-online-mmrpg
chmod +x scripts/*.sh
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
./scripts/iniciar.sh
```

Abre <http://localhost:8081>. Utiliza Java 17 o 21; el script prepara las imágenes incluidas y pregunta la contraseña root si no existe una cuenta administrativa. No requiere Ollama para jugar. Consulta [ARRANQUE-Y-PUERTOS.md](ARRANQUE-Y-PUERTOS.md) si hay otra aplicación en 8080/8081, usas Docker o desarrollas con Angular en 4200.

## Actualizar una copia clonada

Antes de actualizar una instalación con partidas, detén Java y crea un respaldo privado de `data/` y `world/` juntos. Puedes usar `./scripts/respaldo.sh`; revisa también las instrucciones para volúmenes en [OPERACION.md](OPERACION.md).

Desde la copia del repositorio:

```bash
git status --short
git pull --ff-only origin main
```

Si solo recibes esta actualización documental, no es necesario recompilar ni reiniciar el juego. Si una actualización posterior cambia las fuentes, recompila con `./scripts/compilar.sh` antes de arrancar. Este script ejecuta las pruebas del cliente, el build Angular y Maven `verify`.

Si Git informa cambios locales o ramas divergentes, resuélvelos antes de continuar. `world/world.json` está versionado y puede haber sido editado desde root; conserva tu mundo y revisa cualquier conflicto. Evita `git reset --hard`, `git clean` o sobrescribir la carpeta completa como procedimiento de actualización de una partida.

Conserva `.env`, `data/`, el mundo editado y los volúmenes. En Docker utiliza `docker compose up --build -d`; no borres volúmenes para actualizar.

## Preparar otra publicación

1. Revisa `git status` y selecciona únicamente los cambios del proyecto. `data/`, `.env`, cachés y dependencias instaladas están excluidos por `.gitignore`. La carpeta local `backups/` no está excluida actualmente por ese archivo: no añadas respaldos privados al repositorio.
2. Si cambias código, ejecuta `./scripts/compilar.sh` y las pruebas adicionales de los flujos afectados. Registra exactamente qué ejecutaste y sus límites en [VALIDACION.md](VALIDACION.md).
3. Si distribuyes una nueva versión ejecutable, incluye el JAR actualizado y el cliente compilado. No cambies el número de versión únicamente por editar documentación.
4. Regenera y verifica el paquete, y revisa los archivos preparados antes del commit y push.

El directorio `client/dist/` está en `.gitignore`, aunque el compilado inicial está versionado. Un build Angular puede producir nombres nuevos: `git add -u client/dist` registra los archivos eliminados/modificados ya seguidos y `git add -f client/dist` permite incluir los nuevos. Excluye las imágenes duplicadas del directorio servido al prepararlo, conservando las originales en `client/public/assets/`:

```bash
git add -u client/dist
git add -f -- client/dist ':(exclude)client/dist/aincrad/browser/assets/**'
git add release/aincrad-server-0.2.0.jar
git diff --cached --stat
```

Adapta la ruta del JAR si has cambiado la versión. Estos comandos preparan los compilados; añade por separado las fuentes y la documentación que hayas modificado. No utilices `git add .` sobre una instalación que contenga respaldos privados.

## Empaquetar y comprobar integridad

El empaquetador requiere fuentes, JAR, cliente construido, mundo y documentación presentes. Después de validar los cambios:

```bash
python3 scripts/empaquetar.py
sha256sum -c SHA256SUMS.txt
```

El ZIP se crea en la carpeta superior con el nombre `aincrad-seed-v0.2.0.zip` y raíz interna `aincrad-seed/`. Si ya existe un ZIP con ese nombre, se reemplaza: conserva cualquier entrega anterior que necesites antes de empaquetar.

El script regenera `SHA256SUMS.txt` y excluye `.git`, `.env`, `data`, `backups`, dependencias, cachés y las imágenes duplicadas de `client/dist/aincrad/browser/assets/`. Las imágenes originales siguen incluidas en `client/public/assets/`. **El mundo de `world/world.json` sí se empaqueta**: prepara la distribución desde una copia limpia, sin un mundo privado modificado durante una partida.

La verificación de hashes corresponde a los archivos de esa entrega. Una modificación local, incluidas las guías o el mundo, puede producir una diferencia legítima; comprueba una copia recién obtenida antes de ejecutar el juego. Al modificar y volver a distribuir archivos, regenera el manifiesto. Para entregar un ZIP grande en GitHub utiliza un adjunto de Releases; el paquete original supera el límite de 100 MiB por archivo de Git normal. [Límites oficiales de GitHub](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github).

## Validación y límites

La coincidencia de los archivos confirma la subida y no equivale a una partida completa, una prueba de carga ni una nueva ejecución de toda la batería de pruebas. La partida completa en Ubuntu 26.04 fue realizada y confirmada por el usuario el 29 de septiembre de 2026. Ese resultado manual y las comprobaciones automatizadas están registrados en [VALIDACION.md](VALIDACION.md). Siguen pendientes la validación de una ejecución masiva en producción y la confirmación de entrega de correo a un proveedor externo.
