# Operación y configuración

Para seleccionar el puerto local de Spring Boot, configurar 8081 en Docker o ajustar el proxy Angular consulta [ARRANQUE-Y-PUERTOS.md](ARRANQUE-Y-PUERTOS.md). La descarga, actualización por Git y generación del ZIP están en [PUBLICACION.md](PUBLICACION.md).

## Archivos persistentes

| Archivo / volumen | Contenido |
|---|---|
| `world/world.json` / `game-world` | Mundo editado, semilla y catálogos de personajes/especies y zonas |
| `data/admin.json` / `game-data` | Hash de contraseña root |
| `data/accounts.json` / `game-data` | Usuarios y contraseñas derivadas |
| `data/players/` / `game-data` | Personajes, propietario (`accountId`), apariencia y progreso |
| `data/npc-ai.json` / `game-data` | Ollama y personalidades de NPC |
| `data/backups/` / `game-data` | Versiones previas de mundo, configuración IA y personajes |
| `ollama-data` | Modelos descargados en el servicio opcional |

El ZIP de distribución excluye estos datos locales, contraseñas, `.env`, cachés y `node_modules`. No hay SQL, PostgreSQL ni Redis en v0.2.0: se eligió persistencia de archivos para que la semilla arranque con pocas dependencias.

## Variables de entorno del servidor

| Variable | Valor inicial |
|---|---|
| `PORT` | 8080 |
| `BIND_ADDRESS` | 127.0.0.1 local; 0.0.0.0 dentro del contenedor |
| `WORLD_FILE` | ./world/world.json |
| `DATA_DIR` | ./data |
| `ROOT_PASSWORD` | Sin valor; obligatoria solo en primer inicio |
| `ALLOWED_ORIGINS` | localhost/127.0.0.1:8080 y :4200 en Java; Compose publica :8080 |
| `APP_PUBLIC_URL` | http://localhost:8080; URL de los enlaces de recuperación |
| `PLAYER_REGISTRATION_ENABLED` | true; false cierra nuevos registros |
| `PLAYER_SESSION_HOURS` | 12; rango 1–168 |
| `PLAYER_COOKIE_SECURE` | false en HTTP local; true para HTTPS público |

El panel y el WebSocket son del mismo origen que la web. Si cambias puerto o IP, incluye el origen exacto (esquema, IP/nombre y puerto) en `ALLOWED_ORIGINS`. No uses comodines.

En ejecución local `scripts/iniciar.sh` recibe las variables por entorno; no carga `.env` ni reenvía argumentos a Java. Compose sí utiliza `.env` para su configuración: `WEB_PORT` cambia el puerto publicado en el equipo y `HOST_BIND` la dirección de publicación, mientras Java permanece en 8080 dentro del contenedor. `APP_PUBLIC_URL` debe apuntar a la URL que abren los jugadores.

LAN local, ejemplo:

```bash
BIND_ADDRESS=0.0.0.0 \
ALLOWED_ORIGINS=http://192.168.1.50:8080,http://localhost:8080 \
./scripts/iniciar.sh
```

Sustituye la IP por la del equipo. En Compose ajusta `HOST_BIND=0.0.0.0` y los mismos orígenes en `.env`. Para exponerlo fuera de una red confiable hacen falta HTTPS/WSS, reverse proxy, `PLAYER_COOKIE_SECURE=true` y pruebas de carga adicionales. No es un servidor público endurecido.

## Respaldo local

Detén el servidor y ejecuta `./scripts/respaldo.sh`. Incluye `data` y `world`; el archivo es privado. Para restaurar, con el servidor detenido, extrae ese respaldo sobre una copia del proyecto y conserva ambas carpetas. No combines perfiles de una versión posterior con código antiguo sin revisar su esquema.

## Respaldo de Docker

No se requiere instalar un contenedor de terceros para copiar datos:

```bash
mkdir -p backup-docker
docker compose stop game
docker compose cp game:/app/data backup-docker/data
docker compose cp game:/app/world backup-docker/world
docker compose start game
```

Verifica los archivos antes de dar por válido el respaldo. Para restaurar, detén el servicio y usa `docker compose cp` en sentido inverso sobre el contenedor creado, con el contenido correcto y conservando el propietario UID 10001. Haz una copia previa de los volúmenes; no uses `down -v` como mecanismo de restauración.

## Credenciales root

El asistente local lee dos veces la contraseña sin mostrarla. El asistente de Docker la pasa por entorno sin escribirla en `.env`. Tras inicializar el volumen, ya no hace falta proporcionarla. Cambia la contraseña desde `/admin`: se invalidan las sesiones administrativas existentes. El proyecto no incluye una puerta trasera ni recuperación remota de root.

## Actualización

Conserva `data/`, `world/` y `.env`. Reemplaza fuentes/builds, compila y reinicia. En Docker, `docker compose up --build -d` preserva volúmenes y el entrypoint solo copia el mundo inicial si no existe uno guardado. El cliente precompilado del ZIP reutiliza las imágenes de `client/public/assets`: `iniciar.sh` las copia al directorio servido para evitar duplicarlas en el archivo de distribución.

## Cuentas y actualización del acceso

El servidor requiere registro y sesión para jugar. Las cuentas persisten en `data/accounts.json`; sus sesiones se cierran al reiniciar Java. Los personajes anteriores se conservan y deben vincularse tras iniciar sesión, mediante su clave antigua o la administración root. Consulta [CUENTAS.md](CUENTAS.md). No ejecutes el JAR anterior sobre datos de esta entrega, porque no aplica la autorización de cuentas.

## Diagnóstico

`./scripts/diagnostico.sh` muestra versiones, archivos y `/api/health`, sin volcar variables de entorno o credenciales. Si Ollama falla, primero consulta modelos desde el panel y verifica la URL dentro/fuera de Docker. Si el WebSocket devuelve 401, inicia sesión; si devuelve 403, revisa `ALLOWED_ORIGINS` y que el navegador envíe el origen configurado. Si el render falla, activa WebGL 2/aceleración gráfica; si va lento, reduce resolución y densidad de árboles.

## Actualizar desde v0.1.0

Detén Java, realiza una copia privada de `data/` y `world/` juntos, sustituye código/JAR/cliente y conserva esos directorios. El mundo anterior se migra al arrancar, después de validar; guarda una copia del JSON anterior. No sobrescribas tu mundo editado con la semilla nueva si quieres conservarlo. Los mapas antiguos no reciben posiciones forzadas de edificios, nodos, patio o zonas de monstruos; añádelos desde root.

Respaldar solo `players/` ya no es suficiente. `data/pvp-journal/` y `data/harvest-journal/` contienen recibos de bajas/reputación y recolección: restaura siempre el directorio completo junto al mundo. Se conservan sin compactación automática en esta semilla; planifica mantenimiento si la partida crece. El script `respaldo.sh` incluye todo `data` y `world` con el servidor detenido.

La vida/efectos de criaturas y muñecos, invocaciones, mejoras temporales y contadores de entrenamiento se reinician con Java o al aplicar mundo. Las recargas activas/raciales, objetos, oficios, agotamiento de recursos y ciudadanía persisten. Una desconexión en combate PvP mantiene temporalmente al personaje expuesto; el reinicio conserva la marca de combate pendiente.


## Actualizar a zonas de monstruos (esquema 7)

La migración conserva tu mapa anterior y añade el catálogo de especies y `monsterZones: []` donde falte. Los enemigos existentes pasan a nivel 1 conservando los valores base anteriores. Las cinco zonas vienen en la semilla nueva; añádelas desde root si mantienes un mundo guardado. Consulta MONSTRUOS-ZONAS.md para coordenadas orientativas, validación y crecimiento. Al aplicar el mundo se reconstruyen criaturas y temporizadores, sin eliminar el progreso de los personajes.

## Utilidad de correo y mercado

Para correo directo por IP utiliza `./scripts/correo.sh iniciar` y consulta [CORREO.md](CORREO.md). El correo es opcional; la autenticación de jugadores sigue siendo obligatoria aunque esté desactivado. Conserva el volumen `mail-queue` junto a `game-data` y `game-world` al actualizar. No hay contraseña SMTP externa.

La configuración del mercado está en `crafting.shopProducts` y los precios de reventa en `crafting.materials[].sellPrice`. Los materiales y piezas compradas/vendidas se guardan en los perfiles de `data/players/`; no reinicies ese directorio para actualizar el catálogo.
