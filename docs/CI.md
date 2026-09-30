# Integración continua

El workflow [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) ejecuta las comprobaciones automáticas al recibir un `push` en cualquier rama y al abrir o actualizar una PR dirigida a `main`. Después de integrarlo en la rama predeterminada también se puede iniciar desde **Actions → CI → Run workflow**.

Usa runners alojados por GitHub con Ubuntu 24.04, JDK 17 de Temurin y la versión de Node definida en [`.nvmrc`](../.nvmrc), actualmente **22.22.0**. Las acciones están fijadas a SHA completos, con su versión indicada en comentarios. Cada trabajo tiene un límite de 15 minutos; una ejecución nueva cancela la anterior de la misma rama o PR.

## Trabajos y correspondencia con los scripts

| Trabajo | Comprobaciones | Referencia local |
|---|---|---|
| Servidor Spring Boot | `mvn -B -f server/pom.xml verify` | Fase Maven de [`scripts/compilar.sh`](../scripts/compilar.sh) |
| Cliente Angular | `npm --prefix client ci --no-audit --no-fund`, `npm --prefix client test`, `npm --prefix client run build` | Fases npm de [`scripts/compilar.sh`](../scripts/compilar.sh) |
| Cliente Java Desktop | Empaquetar el servidor actual, copiar su JAR a `release/` y ejecutar `bash scripts/compilar-cliente-java.sh`, que llama a `mvn -B -f client-java/pom.xml verify` | [`scripts/compilar-cliente-java.sh`](../scripts/compilar-cliente-java.sh) y copia del JAR en `scripts/compilar.sh` |

Los tres trabajos son independientes: un fallo del servidor no cancela las comprobaciones Angular o Java Desktop. Las fases de instalación, pruebas y build de Angular aparecen como pasos separados. En Java Desktop también se distingue la preparación del servidor de la verificación del cliente.

`ServerContractTest` necesita `release/aincrad-server-0.2.0.jar`. El trabajo Java lo reconstruye con `mvn -B -f server/pom.xml package -DskipTests` y reemplaza **solo en el runner** el JAR incluido en el checkout. Así prueba el protocolo de los fuentes actuales. Las pruebas completas del servidor se ejecutan en el trabajo Servidor Spring Boot; `-DskipTests` solo evita repetirlas durante esta preparación. No se confirma ni publica el JAR generado.

## Datos y cachés

El workflow no carga `.env`, no recibe secretos de instalación y no solicita cuentas ni contraseñas reales. Las pruebas Maven existentes crean sus datos y cuentas sintéticas en directorios temporales. Los contratos del cliente generan su contraseña, eligen un puerto local libre y arrancan/detienen su propio servidor; las pruebas de correo usan un SMTP de prueba local. No necesitan Ollama, un servicio de correo externo ni un servidor de juego preexistente.

Las pruebas npm seleccionadas son las de `npm test` (`node --test tests/*.test.mjs`). La verificación Maven del cliente carga y comprueba geometría sin abrir JavaFX/OpenGL. Se establece `java.awt.headless=true`. No se ejecutan los scripts `test:browser`, pruebas visuales ni `mixed-clients.mjs`, que requieren preparación adicional o cuentas de prueba previas. La CI no sustituye las validaciones manuales documentadas en [VALIDACION.md](VALIDACION.md) y [CLIENTE-JAVA.md](CLIENTE-JAVA.md).

El token del workflow tiene únicamente `contents: read` y checkout no conserva credenciales Git. Se usa `pull_request`, sin ejecutar código de PR mediante `pull_request_target`. No hay pasos de publicación, subida de datos o capturas.

Solo se cachean dependencias: `setup-java` usa el repositorio Maven con claves derivadas de los POM de cada trabajo, y `setup-node` usa la caché de paquetes npm con la clave del `client/package-lock.json`. `npm ci` reconstruye `node_modules` desde el lockfile. No se cachean `data/`, mundos temporales, partidas, respaldos, logs, `.env`, `target/` ni `dist/`.

## Repetir las comprobaciones

Desde la raíz del repositorio, con JDK 17, Maven 3.9.9+ y Node 22.22.0:

```bash
mvn -B -f server/pom.xml verify
npm --prefix client ci --no-audit --no-fund
npm --prefix client test
npm --prefix client run build
mkdir -p release
cp server/target/aincrad-server-0.2.0.jar release/aincrad-server-0.2.0.jar
bash scripts/compilar-cliente-java.sh
```

[`scripts/compilar-todo.sh`](../scripts/compilar-todo.sh) ejecuta esas mismas fases de compilación y verificación en secuencia. En **Actions → CI → ejecución** se pueden consultar los resultados por componente y el paso que falló. La primera ejecución necesita acceso a los repositorios públicos de dependencias; disponer de una caché no omite pruebas ni builds.
