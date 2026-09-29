# Registro, contraseña e inicio de sesión

Para jugar es obligatorio tener una cuenta registrada e iniciar sesión. El servidor comprueba la sesión al abrir el WebSocket y durante la partida. Una clave antigua del navegador, un nombre de personaje o una sesión de root no permiten entrar como jugador.

## Primer acceso

1. Abre <http://localhost:8080> y pulsa **Crear cuenta**.
2. Elige un usuario de 3–32 caracteres: comienza con una letra y admite `a-z`, `0-9`, `_` y `-`. Se normaliza a minúsculas; `root` y `admin` están reservados.
3. Introduce un correo al que tengas acceso. Escribe y repite una contraseña de **12–128 caracteres**. No hay contraseña inicial para jugadores.
4. Tras el registro, usa **Iniciar sesión → Entrar a mi cuenta**.
5. Crea tu personaje o elige uno de **Tus personajes → Continuar aventura**.

Una cuenta puede tener varios personajes. **Crear otro personaje** conserva los anteriores. Desde otro navegador o equipo, inicia sesión con el mismo usuario y contraseña para recuperarlos; ya no necesitas copiar una clave del navegador.

**Salir del mundo** guarda/desconecta el personaje y vuelve a la selección manteniendo tu sesión. **Cerrar sesión** revoca la sesión del servidor, cierra su conexión de juego y vuelve al formulario de acceso. En combate PvP se conservan las reglas existentes: desconectarse no elimina la exposición temporal del personaje.

## Cambiar o recuperar contraseña

En la selección de personajes, pulsa **Cambiar mi contraseña**. Introduce la contraseña actual y confirma la nueva. Se invalidan todas las sesiones de la cuenta y debes iniciar sesión de nuevo.

Si la olvidaste, pulsa **Olvidé mi contraseña**, introduce el correo guardado y abre el enlace recibido. Elige y repite la nueva contraseña; después debes iniciar sesión. El enlace es de un solo uso y caduca a los 30 minutos, configurables. Pedir otro invalida el anterior. El restablecimiento cierra todas las sesiones y conserva los personajes. La respuesta pública es la misma para correos registrados y desconocidos. El servidor debe tener activada la utilidad integrada de [CORREO.md](CORREO.md).

En tu cuenta puedes **Añadir/Cambiar mi correo**, confirmando tu contraseña actual. El cambio cierra las sesiones y anula los enlaces previos. Las cuentas antiguas sin correo conservan su acceso; añade una dirección antes de necesitar recuperación. El registro comprueba el formato, pero no confirma que controles el buzón: revisa la dirección que escribes.

Si el correo no está disponible, el administrador entra en `/admin` como root y utiliza **Usuarios registrados → Cuenta a recuperar → Restablecer contraseña del jugador**. Root define una contraseña nueva; no puede consultar la anterior. El cambio corta también las conexiones abiertas del jugador.

La cuenta root mantiene su contraseña configurada al primer inicio y su sesión administrativa independiente. Para jugar, registra otra cuenta de jugador.

## Actualizar sin perder personajes anteriores

Detén el servidor y respalda **todo `data/` y `world/`**. Sustituye el JAR, cliente compilado y fuentes por esta entrega, conservando esos directorios y la configuración local. No ejecutes simultáneamente dos servidores sobre los mismos datos.

Después de actualizar:

1. Abre el juego en el navegador que usabas antes.
2. Registra una cuenta e inicia sesión.
3. Pulsa **Vincular personaje anterior**. El servidor verifica la clave antigua y que el personaje todavía no pertenezca a una cuenta.
4. El personaje aparecerá en **Tus personajes** con su progreso, inventario, apariencia y oficios. La clave del navegador deja de utilizarse y se retira del almacenamiento local al completar el vínculo.

La vinculación guarda una copia del perfil anterior. No permite apropiarse de un personaje ya vinculado a otra cuenta. Ninguna clave anterior da acceso anónimo al juego.

Si perdiste la clave antigua o conservas varios personajes de versiones anteriores, solicita al administrador la recuperación. Root debe verificar quién es el propietario, seleccionar el personaje en **Personajes del mundo**, elegir **Cuenta del personaje** y pulsar **Vincular personaje a cuenta**. El personaje debe estar desconectado y fuera de combate. La reasignación conserva una copia previa y retira el acceso al propietario anterior. Los nombres de personaje, por sí solos, no acreditan propiedad.

No vuelvas a ejecutar un JAR anterior sobre los datos nuevos: las versiones sin cuentas no aplican estas restricciones. Para volver atrás utiliza una copia completa del código y datos anteriores, con el servidor detenido.

## Persistencia y configuración

| Elemento | Ubicación / comportamiento |
|---|---|
| Usuarios y contraseñas derivadas | `data/accounts.json`; escritura atómica y permisos `0600` cuando el sistema los admite |
| Propietario de cada personaje | `accountId` en su archivo de `data/players/` |
| Sesiones de jugador | Memoria del servidor; se invalidan al reiniciar Java |
| Copias de personajes vinculados/reasignados | `data/backups/` |
| Cuenta root | `data/admin.json`, independiente de los jugadores |

Respalda `accounts.json` junto con el resto de `data/`: copiar sólo `players/` no conserva las cuentas, recibos PvP ni recolección. Los volúmenes de Compose ya incluyen esos archivos; no se añade una base de datos ni dependencias Maven/Node para cuentas.

| Variable | Predeterminado | Efecto |
|---|---|---|
| `PLAYER_REGISTRATION_ENABLED` | `true` | Permite crear cuentas desde la portada. `false` cierra nuevos registros y mantiene el acceso de cuentas existentes. |
| `PLAYER_SESSION_HOURS` | `12` | Duración fija de una sesión; admite 1–168 horas. |
| `PLAYER_COOKIE_SECURE` | `false` | Usa `true` cuando la URL pública es HTTPS, incluido un proxy que termina TLS. También se activa si la solicitud llega al servidor por HTTPS. |
| `ALLOWED_ORIGINS` | Según `OPERACION.md` | Orígenes exactos permitidos para formularios y WebSocket; incluye el esquema, host y puerto visibles en el navegador. |

En ejecución directa, exporta estas variables antes de `./scripts/iniciar.sh`. En Docker Compose están declaradas en `.env.example` y `compose.yaml`; aplícalas recreando el servicio. No hay una opción para permitir jugar sin iniciar sesión. `PLAYER_COOKIE_SECURE=false` permite probar en `http://localhost`; para publicar el juego utiliza HTTPS/WSS y activa la cookie segura.

## Comportamiento de autenticación

- Contraseñas derivadas con **PBKDF2-HMAC-SHA256, 600 000 iteraciones y una sal aleatoria de 16 bytes por cuenta**. No se guardan contraseñas en texto plano.
- Cookie `aincrad_player` con `HttpOnly`, `SameSite=Strict`, ruta `/`, caducidad y `Secure` según la configuración. El cliente no guarda contraseñas ni credenciales nuevas de sesión en `localStorage`.
- Sesiones aleatorias de 256 bits. El servidor conserva su resumen SHA-256, usuario y vencimiento. Cada inicio de sesión emite una sesión nueva y reemplaza la anterior de ese navegador; se admiten hasta ocho sesiones por cuenta, retirando la más antigua al superar el límite.
- Login: 20 intentos/minuto por dirección y ocho por nombre de usuario. Registro: seis/minuto por dirección. Cambio de contraseña: cinco/minuto por cuenta; vinculación antigua: seis/minuto. Los errores de usuario inexistente y contraseña incorrecta son iguales.
- Las mutaciones de jugadores exigen el encabezado `X-Aincrad-Player: 1` y un `Origin` permitido. No se habilita CORS abierto. El WebSocket exige un origen permitido y la cookie de sesión.
- El servidor valida el propietario al seleccionar un personaje; ignora identidades de cuenta suministradas en los mensajes. Las claves antiguas enviadas en `join` se rechazan.
- Cerrar sesión, cambiar/restablecer contraseña o alcanzar la caducidad desconecta las sesiones afectadas con código WebSocket `4401`. La interfaz vuelve al inicio de sesión. Aplicar el mundo desde root conserva las sesiones y solicita reconexión.
- La portada, los recursos gráficos, `/api/health` y el catálogo de configuración `/api/world` siguen siendo públicos y de lectura. No publican contraseñas, sesiones, claves de personajes ni la lista de cuentas. Las operaciones root conservan su autorización independiente.

## API y pruebas

| Método y ruta | Uso |
|---|---|
| `GET /api/player/session` | Consultar la sesión y sus personajes; sin sesión devuelve `authenticated:false` |
| `POST /api/player/register` | `username`, `email`, `password`, `confirmPassword`; crea cuenta, no inicia sesión |
| `POST /api/player/login` | `username`, `password`; establece la cookie |
| `POST /api/player/logout` | Revocar la sesión actual |
| `POST /api/player/password` | `currentPassword`, `password`, `confirmPassword`; requiere sesión |
| `POST /api/player/email` | `currentPassword`, `email`; requiere sesión y cierra las sesiones al cambiar |
| `POST /api/player/forgot-password` | `email`; respuesta genérica y envío asíncrono |
| `POST /api/player/reset-password` | `token`, `password`, `confirmPassword`; consume el enlace |
| `GET /api/admin/mail` | Estado del correo; sólo root |
| `POST /api/admin/mail/test` | `email`; envío de prueba, sólo root |
| `POST /api/player/claim-character` | `legacyToken`; requiere sesión y un personaje sin propietario |
| `GET /api/admin/accounts` | Listado sin contraseñas ni hashes; sólo root |
| `POST /api/admin/accounts/{id}/password` | Restablecer contraseña; sólo root |
| `PATCH /api/admin/characters/{id}/account` | Asignar `accountId`; sólo root |

Después de autenticar, el mensaje WebSocket de entrada es `{"type":"join","characterId":"ID_PROPIO"}`. Para crear personaje omite `characterId` o envíalo vacío, con `name` y `appearance`. El mensaje `welcome` ya no devuelve una clave de acceso. No debes reenviar el antiguo campo `token`.

`mvn -f server/pom.xml verify` incluye pruebas de credenciales, expiración, límites, persistencia, propiedad, migración y accesos HTTP/WebSocket reales. `npm --prefix client run test:accounts` prueba la interfaz contra un servidor aislado con `AINCRAD_ADMIN_PASSWORD`. Admite una fixture privada mediante `AINCRAD_LEGACY_FIXTURE` para comprobar una partida creada con la entrega anterior; nunca uses datos de una partida real en las pruebas automáticas. Resultados y límites en `VALIDACION.md`.

La recuperación admite cinco solicitudes/minuto por dirección y tres por correo, incluyendo direcciones inexistentes. El consumo de enlaces admite diez intentos/minuto por dirección. Un cambio manual de contraseña, un restablecimiento root o un cambio de correo anulan cualquier enlace pendiente. Los tokens de 256 bits sólo autorizan el restablecimiento; no inician sesión. El cliente retira el fragmento del enlace de la barra de direcciones y utiliza `Referrer-Policy: no-referrer` mediante la etiqueta HTML.
