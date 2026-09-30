# Paquete completo del cliente Java

El cliente Java Desktop 0.1.0 para el servidor Aincrad 0.2.0 ya está publicado en `main`. El paquete de fuentes preparado el 30 de septiembre de 2026 se conserva como referencia histórica de distribución; recuperó los fuentes originales, sin reconstruirlos a partir del JAR.

## Publicación en `main` · 30 de septiembre de 2026

| Commit | Cambio publicado |
| --- | --- |
| [`7530b5f`](https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg/commit/7530b5fcbb14ca785b977f4063da27834ea77c63) | Incorporación del cliente Java, documentación y scripts de compilación |
| [`e60cb843`](https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg/commit/e60cb8430eef195bc340be13745c55315e4ed324) | Corrección posterior de recursos Angular y exclusiones del cliente Java |

[`client-java/`](../client-java/) está integrado como carpeta normal del mismo repositorio. No tiene un repositorio Git independiente, no hay `.gitmodules` ni una entrada Git de submódulo. Sus fuentes, pruebas, recursos, guías y scripts ya se obtienen al descargar `main`.

## Contenido del paquete de fuentes original

| Ruta | Contenido y finalidad |
| --- | --- |
| `client-java/src/main/java/` | 21 clases Java del cliente nativo |
| `client-java/src/test/java/` | 3 clases con 27 casos de prueba |
| `client-java/src/main/resources/` | Estilos, manifiesto y geometría original empaquetada |
| `client-java/src/assembly/desktop.xml` | Receta Maven del ZIP ejecutable |
| `client-java/pom.xml` | Dependencias y compilación Java 17 |
| `client-java/tools/` | Exportador de modelos y comprobación mixta Java/Angular |
| `client-java/licenses/`, `NOTICE.md` | Licencias y avisos de dependencias |
| `client/` | Fuentes Angular, lockfile, ilustraciones y compilado original |
| `server/` | Fuentes, configuración Maven y pruebas del servidor |
| `world/` | Mundo y definiciones de ampliación |
| `release/` | JAR originales del servidor; v0.2.0 utilizado por las pruebas Java |
| `docs/` | Toda la documentación recuperada y sus capturas |
| `scripts/` | Compilar, iniciar, diagnosticar, respaldar y empaquetar |

Se conserva la estructura relativa original porque Maven carga ilustraciones desde `../client/public/assets` y el mundo desde `../world`. Copiar solamente `client-java/` fuera del proyecto omitiría esos recursos y el JAR de las pruebas. Los assets duplicados de `client/dist/aincrad/browser/assets/` no se empaquetan; el lanzador del servidor los restaura desde `client/public/assets/`.

## Compilación

Cliente Java con JDK 17/21 y Maven 3.9.9+:

```bash
./scripts/compilar-cliente-java.sh
```

El comando equivale a `mvn -B -f client-java/pom.xml verify`. El código se compila, se ejecutan todas las pruebas y se genera el paquete de escritorio de la plataforma actual. Las pruebas abren servidores temporales en puertos libres y directorios de datos aislados; no utilizan tus partidas.

Para reconstruir servidor, Angular y Java, utiliza `./scripts/compilar-todo.sh` con las dependencias del [README principal](../README.md#compilar-desde-fuentes). Para regenerar intencionalmente el arte, sigue [CLIENTE-JAVA.md](CLIENTE-JAVA.md#arte-reproducible); la compilación Java habitual utiliza los recursos ya incluidos y no ejecuta Node.

## Obtener o actualizar el cliente publicado

Para una instalación nueva, descarga el repositorio completo y compila desde su raíz:

```bash
git clone --branch main https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg.git
cd SAO-Aircraft-online-mmrpg
chmod +x scripts/*.sh client-java/iniciar-cliente.sh
./scripts/compilar-cliente-java.sh
```

En una copia ya clonada, sigue [Actualizar una copia clonada](PUBLICACION.md#actualizar-una-copia-clonada), conservando las partidas y los cambios locales; después compila el cliente Java. El servidor se inicia por separado con sus scripts habituales. Consulta [la guía de uso](../client-java/README.md) para conectar el cliente.

La antigua receta de copiar desde el ZIP con `rsync` y ejecutar `commit`/`push` correspondía a la publicación inicial, ya completada. No es un paso de instalación ni actualización, y no se necesitan comandos de submódulos.

## Referencia de empaquetado

El ZIP histórico `SAO-Java-Desktop-0.1.0-fuentes-completo.zip` tiene como raíz `SAO-Java-Desktop-0.1.0-fuentes/`. Sus instrucciones de extracción están en [README-FUENTES-JAVA.md](../README-FUENTES-JAVA.md#alternativa-histórica-paquete-de-fuentes-del-30-de-septiembre-de-2026); no incorpora automáticamente las correcciones posteriores de `main`.

Para preparar otra instantánea desde una copia limpia del proyecto, con los recursos de la tabla presentes:

```bash
python3 scripts/empaquetar-fuentes-java.py
sha256sum -c SHA256SUMS.txt
```

El script regenera el manifiesto para los archivos que empaqueta y escribe el ZIP en la carpeta superior, con el mismo nombre y raíz del paquete histórico. Si el ZIP ya existe se reemplaza; conserva las entregas anteriores que necesites. El manifiesto versionado del paquete original describe esa entrega, no los cambios posteriores del repositorio. Crear un ZIP no modifica la publicación en GitHub.

## Alcance de validación

La entrega Java documenta comprobaciones visuales Java/Angular en Linux x86_64 con OpenGL por software. La validación del paquete original del 30 de septiembre de 2026 comprobó fuentes, integridad y compilación del cliente desde una extracción nueva. Los resultados están en [VALIDACION-PAQUETE-JAVA.md](VALIDACION-PAQUETE-JAVA.md) y [CLIENTE-JAVA.md](CLIENTE-JAVA.md#validación-reproducible).

Esta actualización documental no repite esas pruebas ni añade plataformas comprobadas. Siguen pendientes la validación visual en GPU física y las ejecuciones gráficas de Windows/macOS. La [partida completa en Ubuntu 26.04](VALIDACION.md#validación-manual-en-ubuntu-2604--confirmación-del-usuario), confirmada por el usuario el 29 de septiembre de 2026, corresponde a la entrega web y no acredita una partida completa con el nuevo cliente Java.
