# Procedencia y dependencias

El cliente Java reutiliza el arte original de este mismo proyecto. `art/models.json.gz` contiene una exportación de `client/src/app/avatar-model.js`, `environment-model.js`, `creature-model.js` y `render-kit.js`, con sus reglas de equipo. No contiene modelos de terceros, recursos extraídos de SAO ni descargas de arte durante la partida. Las ilustraciones proceden de `client/public/assets`; su documentación se incluye en el JAR como `assets/ASSETS.md`.

La exportación se realiza sólo para construir recursos. El programa distribuido interpreta mallas, materiales y articulaciones mediante Java; no ejecuta el código JavaScript del cliente Angular.

El paquete incluye estas dependencias, que conservan sus titulares y términos propios:

| Componente | Versión | Licencia / fuente |
| --- | --- | --- |
| OpenJFX | 21.0.8 | GPL v2 con excepción de classpath; https://github.com/openjdk/jfx |
| jMonkeyEngine | 3.8.1-stable | BSD 3 cláusulas; https://github.com/jMonkeyEngine/jmonkeyengine |
| LWJGL | 3.3.6 | BSD 3 cláusulas y licencias de bibliotecas nativas; https://github.com/LWJGL/lwjgl3 |
| Jackson | 2.19.2 | Apache 2.0; https://github.com/FasterXML/jackson |

Los textos de licencia adicionales están en `licenses/`; los JAR originales conservan sus archivos `META-INF` y avisos. JUnit se utiliza para pruebas y no se incluye en el paquete ejecutable. La inclusión de estas bibliotecas no convierte el proyecto en un producto oficial de Sword Art Online.
