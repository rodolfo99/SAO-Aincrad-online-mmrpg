# SAO Java Desktop 0.1.0 — paquete completo de fuentes

Esta entrega contiene el código fuente original del cliente Java, sus recursos gráficos, pruebas, documentación y scripts de compilación. También incluye los fuentes de Angular y del servidor, el mundo y los JAR originales del servidor: el POM Java usa ilustraciones de `client/public/assets/`, y sus pruebas de contrato necesitan `world/world.json` y `release/aincrad-server-0.2.0.jar`.

## Descomprimir y compilar el cliente Java

Necesitas un JDK 17 o 21 y Maven 3.9.9 o posterior. Maven descarga las dependencias en la primera compilación; se requiere Internet. Node.js no es necesario para compilar el cliente Java porque el arte ya está incluido.

```bash
unzip SAO-Java-Desktop-0.1.0-fuentes-completo.zip
cd SAO-Java-Desktop-0.1.0-fuentes
chmod +x scripts/*.sh client-java/iniciar-cliente.sh
./scripts/compilar-cliente-java.sh
```

La compilación ejecuta 27 pruebas y genera:

- `client-java/target/aincrad-client-java-0.1.0.jar`
- `client-java/target/lib/`
- `client-java/target/aincrad-client-java-0.1.0-desktop.zip`

Ese último ZIP es el paquete ejecutable de la plataforma donde compilas. Este archivo de fuentes es independiente del ZIP ejecutable Linux entregado anteriormente.

## Ejecutar

Inicia el servidor en una terminal. El primer arranque solicitará la contraseña root:

```bash
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
./scripts/iniciar.sh
```

En otra terminal, desde la misma carpeta:

```bash
./scripts/iniciar-cliente-java.sh http://localhost:8081
```

También puedes conectar el cliente a tu servidor existente indicando su dirección. Utiliza la misma cuenta que en Angular. El cliente no incorpora un servidor ni una copia de tus partidas.

## Compilar todos los componentes

Además de Java y Maven, instala una versión de Node.js compatible con el README principal (la versión fijada es 22.22.0). Después ejecuta:

```bash
./scripts/compilar-todo.sh
```

El script compila y prueba Angular y el servidor mediante `compilar.sh`, actualiza el JAR del servidor y después compila y prueba el cliente Java. Esa reconstrucción activa las rutas autoritativas por clic presentes en los fuentes. El JAR v0.2.0 precompilado original conserva el movimiento anterior.

## Documentación y publicación

- [Guía detallada del cliente](client-java/README.md)
- [Funciones, protocolo, gráficos, plataformas y pruebas](docs/CLIENTE-JAVA.md)
- [Contenido del paquete y cómo subir el módulo a GitHub](docs/PAQUETE-FUENTES-JAVA.md)
- [Comprobaciones de este paquete](docs/VALIDACION-PAQUETE-JAVA.md)
- [Documentación general](README.md)
- [Avisos y licencias](client-java/NOTICE.md)

El ZIP no incluye `.git`, datos de partidas, contraseñas, `.env`, cachés, `node_modules` ni directorios `target`. `SHA256SUMS.txt` permite comprobar los archivos incluidos:

```bash
sha256sum -c SHA256SUMS.txt
```

Para repetir el empaquetado después de modificar los fuentes:

```bash
python3 scripts/empaquetar-fuentes-java.py
```

El ZIP resultante se guarda junto a la carpeta del proyecto. Tener este paquete no confirma que el cliente Java ya esté publicado en `main`; la publicación se realiza aparte con Git.
