# SAO Java Desktop 0.1.0 — paquete completo de fuentes

**Cliente Java publicado en `main` el 30 de septiembre de 2026:** incorporado en [`7530b5f`](https://github.com/rodolfo99/SAO-Aincrad-online-mmrpg/commit/7530b5fcbb14ca785b977f4063da27834ea77c63), seguido por la corrección [`e60cb843`](https://github.com/rodolfo99/SAO-Aincrad-online-mmrpg/commit/e60cb8430eef195bc340be13745c55315e4ed324) de recursos Angular y exclusiones del cliente Java. `client-java/` es una carpeta normal del repositorio, con fuentes, recursos, pruebas, documentación y scripts; no es un submódulo y no existe `.gitmodules`.

Esta entrega contiene el código fuente original del cliente Java, sus recursos gráficos, pruebas, documentación y scripts de compilación. También incluye los fuentes de Angular y del servidor, el mundo y los JAR originales del servidor: el POM Java usa ilustraciones de `client/public/assets/`, y sus pruebas de contrato necesitan `world/world.json` y `release/aincrad-server-0.2.0.jar`.

## Obtener y compilar la versión publicada

Necesitas un JDK 17 o 21 y Maven 3.9.9 o posterior. Maven descarga las dependencias en la primera compilación; se requiere Internet. Node.js no es necesario para compilar el cliente Java porque el arte ya está incluido.

```bash
git clone --branch main https://github.com/rodolfo99/SAO-Aincrad-online-mmrpg.git
cd SAO-Aincrad-online-mmrpg
chmod +x scripts/*.sh client-java/iniciar-cliente.sh
./scripts/compilar-cliente-java.sh
```

Para actualizar una copia clonada, consulta [Publicación y actualización](docs/PUBLICACION.md#actualizar-una-copia-clonada). No se necesita `git submodule update` ni copiar el cliente desde un ZIP.

### Alternativa histórica: paquete de fuentes del 30 de septiembre de 2026

Estas instrucciones siguen siendo útiles si conservas `SAO-Java-Desktop-0.1.0-fuentes-completo.zip`. Ese ZIP es una instantánea de su preparación; para obtener la documentación y correcciones posteriores utiliza `main`.

```bash
unzip SAO-Java-Desktop-0.1.0-fuentes-completo.zip
cd SAO-Java-Desktop-0.1.0-fuentes
chmod +x scripts/*.sh client-java/iniciar-cliente.sh
./scripts/compilar-cliente-java.sh
```

En ambos casos, la compilación ejecuta las 27 pruebas existentes y genera:

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
- [Estado publicado y referencia de empaquetado](docs/PAQUETE-FUENTES-JAVA.md)
- [Comprobaciones históricas del paquete de fuentes](docs/VALIDACION-PAQUETE-JAVA.md)
- [Documentación general](README.md)
- [Avisos y licencias](client-java/NOTICE.md)

## Empaquetado e integridad

El ZIP no incluye `.git`, datos de partidas, contraseñas, `.env`, cachés, `node_modules` ni directorios `target`. Dentro de una extracción sin modificar, su `SHA256SUMS.txt` permite comprobar los archivos de esa entrega:

```bash
sha256sum -c SHA256SUMS.txt
```

El manifiesto versionado procede del paquete original y no certifica los cambios posteriores de `main`. Para preparar otra distribución desde una copia limpia con los recursos necesarios, el script regenera el manifiesto y el ZIP:

```bash
python3 scripts/empaquetar-fuentes-java.py
sha256sum -c SHA256SUMS.txt
```

El ZIP resultante se guarda junto a la carpeta del proyecto con el mismo nombre; conserva una entrega anterior antes de reemplazarla. Empaquetar sirve para distribuir una instantánea de los archivos locales. El cliente Java ya está publicado en `main`; no queda pendiente una subida inicial.
