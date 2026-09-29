# Arranque y puertos de Aincrad v0.2.0

Esta guía corresponde a los scripts y a `server/src/main/resources/application.properties` publicados en GitHub. Para obtener o actualizar el proyecto consulta [PUBLICACION.md](PUBLICACION.md).

## Qué escucha en cada puerto

| Ejecución | Puerto del navegador | Puerto de Java | Configuración |
|---|---|---|---|
| JAR local, valor predeterminado | 8080 | 8080 | `PORT=8080` |
| JAR local, ejemplo de esta guía | 8081 | 8081 | `PORT=8081` |
| Angular en desarrollo | 4200 | 8081 en este ejemplo | Proxy `/api` y `/ws` hacia Java |
| Docker Compose con puerto alternativo | 8081 en el equipo | 8080 dentro del contenedor | `WEB_PORT=8081` |

Java sirve el cliente compilado, la API y el WebSocket. Para jugar con la entrega precompilada no es necesario ejecutar Angular por separado. Cambiar el puerto del navegador o del proxy no cambia por sí mismo el puerto de Spring Boot.

## Arranque local en 8081

Instala Java 17 o 21. Entra en la raíz del proyecto, donde están `release/`, `client/`, `world/` y `scripts/`:

```bash
java -version
chmod +x scripts/*.sh
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
./scripts/iniciar.sh
```

- Juego: <http://localhost:8081>.
- Administración: <http://localhost:8081/admin>, usuario `root`.
- Salud del servidor: <http://localhost:8081/api/health>.
- Detención ordenada: `Ctrl+C` en la terminal que ejecuta Java.

En un directorio de datos nuevo, el asistente solicita dos veces una contraseña root de 12–128 caracteres. Si ya existe `data/admin.json`, conserva esa cuenta; otra variable `ROOT_PASSWORD` no reemplaza su contraseña. El registro de jugadores es independiente de root.

El script copia las imágenes de `client/public/assets/` al directorio servido. Si falta el JAR o `client/dist/aincrad/browser/index.html`, intenta compilar: en ese caso sí necesitas JDK, Maven y Node.js compatibles con el [README](../README.md).

### Las variables tienen funciones distintas

| Variable | Función |
|---|---|
| `PORT` | Puerto local de Spring Boot; por defecto 8080 |
| `BIND_ADDRESS` | Dirección de escucha; por defecto `127.0.0.1` |
| `ALLOWED_ORIGINS` | Orígenes exactos permitidos para cuentas, administración y WebSocket; incluye esquema y puerto |
| `APP_PUBLIC_URL` | URL utilizada para construir enlaces de recuperación; no cambia la escucha de Java |
| `DATA_DIR` / `WORLD_FILE` | Directorio de datos y archivo del mundo |

`scripts/iniciar.sh` **no lee `.env` automáticamente**. Las asignaciones anteriores se transmiten al proceso de ese arranque. Para reutilizar variables en la misma terminal puedes exportarlas. No publiques `.env`, cuentas, partidas ni respaldos.

## Imponer el puerto directamente a Spring Boot

El script no contiene `"$@"` en la llamada a Java: `./scripts/iniciar.sh --server.port=8081` no reenvía ese argumento. Para usar una opción explícita de Spring Boot, ejecuta el JAR desde la raíz del proyecto.

En el primer inicio, prepara la contraseña sin escribirla en el historial:

```bash
read -r -s -p 'Contraseña root de 12–128 caracteres: ' ROOT_PASSWORD
printf '\n'
export ROOT_PASSWORD
```

Si `data/admin.json` ya existe, ese paso no es necesario. Prepara las imágenes y arranca:

```bash
mkdir -p client/dist/aincrad/browser/assets
cp -a client/public/assets/. client/dist/aincrad/browser/assets/

ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
java -Dfile.encoding=UTF-8 \
  -jar release/aincrad-server-0.2.0.jar \
  --server.port=8081
```

Después de detener Java, elimina la contraseña del entorno de esa terminal con `unset ROOT_PASSWORD`. Ejecutar el JAR desde otra carpeta cambia las rutas relativas de cliente, datos y mundo; utiliza la raíz del proyecto o configura explícitamente esas rutas.

Cambiar el `.properties` de las fuentes no modifica un JAR ya construido. Para incorporar ese cambio recompila; para seleccionar el puerto de ejecución usa el entorno o el argumento anterior. Si configuras también `SERVER_PORT`, `JAVA_TOOL_OPTIONS` o argumentos `--server.port`, revisa sus valores para no mantener configuraciones contradictorias.

## Confirmar el puerto real y resolver un puerto ocupado

En otra terminal:

```bash
ss -ltnp '( sport = :8080 or sport = :8081 )'
curl --fail --silent --show-error http://127.0.0.1:8081/api/health
PORT=8081 ./scripts/diagnostico.sh
```

La API debe responder con `status: "ok"` y `version: "0.2.0"`. El texto `Juego: ...` impreso por el script es orientativo y aparece antes de arrancar Java; confirma la escucha con `ss` y la API. Para mostrar los mensajes de Tomcat al ejecutar el JAR directamente, puedes añadir `--logging.level.org.springframework.boot.web.embedded.tomcat=INFO` y comprobar la línea que anuncia el puerto iniciado.

| Síntoma | Comprobación y acción |
|---|---|
| `BindException`, `Address already in use` o `La dirección ya se está usando` | Consulta `ss`; detén de forma ordenada la instancia identificada o selecciona otro puerto libre |
| `/api/health` no responde en 8081 | Revisa si Java terminó, en qué puerto escucha y si configuraste el proceso Java correcto |
| Funciona `/api/health` pero no aparece la web | Comprueba el directorio de ejecución, `client/dist/aincrad/browser/index.html` y la copia de imágenes |
| HTTP 403 u origen rechazado | Incluye en `ALLOWED_ORIGINS` el origen exacto usado por el navegador y reinicia Java |
| WebSocket 401 | Registra una cuenta e inicia sesión antes de entrar al mundo |
| El enlace de recuperación apunta a 8080 | Configura `APP_PUBLIC_URL` con la URL accesible del juego y reinicia |

No elimines `data/` ni `world/` para resolver un conflicto de puertos. El proceso que ocupa un puerto puede pertenecer a otra aplicación; identifica la instancia antes de detenerla.

## Angular en desarrollo con Java en 8081

En una terminal inicia Java permitiendo también el origen de desarrollo:

```bash
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081,http://localhost:4200,http://127.0.0.1:4200 \
APP_PUBLIC_URL=http://localhost:4200 \
./scripts/iniciar.sh
```

En `client/proxy.conf.json` configura ambas rutas; el valor publicado inicialmente apunta a 8080:

```json
{
  "/api": { "target": "http://127.0.0.1:8081", "secure": false },
  "/ws": { "target": "http://127.0.0.1:8081", "secure": false, "ws": true }
}
```

En otra terminal, desde la raíz del proyecto:

```bash
cd client
npm ci
npm start
```

Abre <http://localhost:4200>. Reinicia `npm start` después de cambiar el proxy. Usa de forma consistente el mismo nombre de host al registrarte, iniciar sesión y abrir el juego, porque las sesiones usan cookies. `APP_PUBLIC_URL` apunta a 4200 en este ejemplo para que los enlaces de recuperación abran el cliente de desarrollo.

## Docker Compose: 8081 fuera, 8080 dentro

Si todavía no tienes `.env`, crea la configuración inicial:

```bash
cp -n .env.example .env
```

Edita estas líneas de `.env`, conservando las demás opciones existentes:

```dotenv
HOST_BIND=127.0.0.1
WEB_PORT=8081
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081
APP_PUBLIC_URL=http://localhost:8081
```

Inicia y comprueba:

```bash
./scripts/iniciar-docker.sh
docker compose ps
docker compose port game 8080
docker compose logs --tail=100 game
curl --fail --silent --show-error http://127.0.0.1:8081/api/health
```

Compose publica `${HOST_BIND}:${WEB_PORT}:8080`; el Dockerfile mantiene Java en 8080 y escucha dentro del contenedor en `0.0.0.0`. No necesitas añadir `PORT=8081` a `.env` para este caso. El asistente solicita la contraseña de inicialización; los volúmenes ya creados conservan root y las partidas.

Para aplicar cambios de configuración o código usa `docker compose up --build -d`. `docker compose down` conserva los volúmenes; añadir `-v` los elimina y no es un procedimiento de actualización.

## Correo y acceso desde otro equipo

Con la utilidad local de correo ya configurada según [CORREO.md](CORREO.md), puedes arrancar en el puerto alternativo:

```bash
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
./scripts/iniciar-con-correo.sh
```

Ese asistente lee de `.env` una lista concreta de opciones de correo y cuentas, pero no importa `PORT` ni `WEB_PORT`: pasa `PORT` por entorno. El SMTP local usa 2525 y es independiente del puerto web.

Para jugar desde otro equipo, `localhost` no identifica el servidor: configura la dirección y los orígenes de la LAN como indica [OPERACION.md](OPERACION.md), y utiliza esa URL accesible también en `APP_PUBLIC_URL`. Los límites de validación, de correo externo y de exposición pública están documentados en [VALIDACION.md](VALIDACION.md).
