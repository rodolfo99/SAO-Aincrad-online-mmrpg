# Correo integrado por IP

Aincrad incluye **Postfix** como utilidad de salida y una interfaz Java `MailService` reutilizable por los módulos del servidor. La recuperación de cuentas la usa automáticamente. El remitente es `noreply@[IP_DE_LA_MÁQUINA]` y la conexión de salida utiliza **TCP 25**. No necesitas contratar un servicio SMTP ni registrar un dominio propio.

La entrega final depende del destinatario: algunos proveedores rechazan remitentes por IP, direcciones residenciales o IP sin reputación. Postfix conserva la cola y los motivos de aplazamiento/rechazo. Que Java o la cola local acepten un mensaje **no confirma que llegue al buzón**. No se incluye SPF/DKIM: requieren publicar registros de un dominio, que este modo no exige.

## Activar con Docker Compose

Prepara `.env` como se explica en README y define la contraseña root si es el primer inicio. Después, desde la carpeta del proyecto:

```bash
chmod +x scripts/*.sh
./scripts/correo.sh iniciar
```

Este comando detecta la IPv4 de salida, guarda la configuración y arranca el juego y el servicio `mail`. La detección consulta `https://api.ipify.org` con una petición vacía; no envía destinatarios, mensajes ni contraseñas. Si no puede consultarlo, utiliza la dirección de la interfaz de salida. Una IP privada sólo sirve en su red: detrás de NAT debes utilizar la **IP pública de salida**.

Puedes indicar la dirección sin usar detección:

```bash
./scripts/correo.sh iniciar --ip 203.0.113.10 --url http://203.0.113.10:8080
```

Sustituye `203.0.113.10` por tu IP real; es una dirección de documentación. La URL debe ser la del juego accesible para los jugadores. Si cambias el puerto o usas HTTPS por IP, indícalo con `--url`. **HTTP no cifra las contraseñas ni los enlaces de recuperación**; para publicar el juego configura HTTPS por IP y usa la URL `https://…`.

El asistente conserva las demás opciones de `.env`, crea una copia privada en `backups/` y añade el origen del juego a `ALLOWED_ORIGINS`. La contraseña root no se muestra ni se cambia. `HOST_BIND` sigue respetando tu configuración: para conexiones desde fuera de la máquina cambia el enlace web como indica README. No abre el firewall del alojamiento.

## Prueba, cola y diagnóstico

En `/admin`, root dispone de **Recuperación de cuentas → Correo de prueba → Enviar correo de prueba**. También puedes utilizar la terminal:

```bash
./scripts/correo.sh prueba destinatario@correo.com
./scripts/correo.sh cola
./scripts/correo.sh registro
./scripts/correo.sh reintentar
```

`prueba` compone un mensaje sencillo y lo entrega a Postfix; no incluye contraseñas ni enlaces de cuentas. `cola` muestra pendientes y sus motivos; `registro` muestra las últimas decisiones SMTP; `reintentar` solicita otro intento para los pendientes. Un registro `status=sent` significa que el siguiente servidor aceptó el mensaje; el usuario debe comprobar bandeja de entrada y spam. `deferred` indica aplazamiento y `bounced`, rechazo definitivo.

No se publica el puerto SMTP hacia Internet. Postfix sólo admite reenvío desde la red privada de los servicios. El alojamiento necesita permitir **conexiones salientes** al puerto 25 y la resolución normal de los servidores de correo destinatarios. No requiere un servidor DNS propio. Si el proveedor bloquea la salida 25, el código del juego no puede eliminar ese bloqueo.

## Ejecutar Java directamente

Si utilizas el JAR en el host, conserva la utilidad Postfix en Docker:

```bash
./scripts/correo.sh iniciar-local --ip TU_IP --url http://TU_IP:8080
./scripts/iniciar-con-correo.sh
```

La variante local publica SMTP **sólo en `127.0.0.1:2525`** para Java; Postfix sigue saliendo al destinatario por el 25. `iniciar-con-correo.sh` lee las opciones admitidas de `.env` como datos, sin ejecutarlo como un script. No sustituye tu instalación de correo del sistema. Docker es necesario para esta utilidad; el juego sin correo sigue ejecutándose con Java solamente.

## Opciones y persistencia

| Opción | Uso |
|---|---|
| `MAIL_ENABLED` | Activa la recuperación y el correo de prueba; inicialmente `false` |
| `MAIL_PUBLIC_IP` | IPv4 real de salida usada en el remitente y saludo SMTP |
| `MAIL_FROM` | El asistente genera `noreply@[IP]` |
| `APP_PUBLIC_URL` | Origen del juego para construir enlaces; no se toma de encabezados del visitante |
| `PASSWORD_RESET_MINUTES` | 30 minutos por defecto; permitido 5–60 |
| `MAIL_NETWORK_SUBNET` | Red privada de correo, por defecto `172.30.42.0/28`; cambia si coincide con otra red |
| `MAIL_HOST`, `MAIL_PORT` | Conexión interna de Java; Compose usa `mail:25`, el JAR local `127.0.0.1:2525` |

La cola se conserva en el volumen `mail-queue`. Contiene los mensajes pendientes, incluidos sus enlaces, por lo que debe tratarse como información privada. Los tokens se almacenan **como hashes en `accounts.json`**; su caducidad no se prolonga si el correo se retrasa. Las cuentas y personajes continúan en `game-data`; el mundo, en `game-world`. No uses `docker compose down -v` para actualizar conservando esos datos.

La utilidad limita el tamaño de mensajes y conexiones. Java utiliza tiempos máximos de conexión/lectura/escritura y una cola acotada de solicitudes. Las claves de recuperación no se muestran en respuestas API, registros de la aplicación ni pantallas administrativas.

## Comprobación de esta entrega

Se prueba registro, solicitud de recuperación, envío SMTP real hacia un receptor local aislado, consumo del enlace, revocación de sesiones y conservación del personaje. El receptor de pruebas no reenvía mensajes a Internet.

En este entorno no hay Docker Engine, por lo que no se arrancaron los contenedores Postfix. Se verificaron configuración, scripts y restricciones de red. La prueba solicitada hacia Hotmail no pudo salir: se resolvió su MX y la conexión al puerto 25 devolvió `Network is unreachable`; **no se envió ningún mensaje externo**. Hay que ejecutar la prueba desde el servidor de destino para evaluar su conectividad y recepción.

Referencias de implementación: [Postfix: configuración](https://www.postfix.org/postconf.5.html), [control de reenvío](https://www.postfix.org/SMTPD_ACCESS_README.html), [TLS saliente](https://www.postfix.org/TLS_README.html), [correo en Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/io/email.html) y [recuperación de contraseñas OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
