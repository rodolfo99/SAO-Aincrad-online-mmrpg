# Paquete completo del cliente Java

Preparado el 30 de septiembre de 2026. Cliente Java Desktop 0.1.0 y servidor Aincrad 0.2.0. Esta entrega recupera los fuentes originales; no se reconstruyeron a partir del JAR.

## Contenido

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

Para reconstruir servidor, Angular y Java, utiliza `./scripts/compilar-todo.sh` con las dependencias del README principal. Para regenerar intencionalmente el arte, sigue `docs/CLIENTE-JAVA.md`; la compilación Java habitual utiliza los recursos ya incluidos y no ejecuta Node.

## Subir el módulo a `main`

El cliente se integra como carpeta del mismo repositorio, sin un repositorio Git independiente. Ejecuta desde la carpeta descomprimida:

```bash
SAO_FUENTES="$PWD"
cd ..
git clone --branch main https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg.git SAO-subida-java
cd SAO-subida-java

rsync -av --exclude='.git' --exclude='target' "$SAO_FUENTES/client-java/" client-java/
cp "$SAO_FUENTES/scripts/compilar-cliente-java.sh" scripts/
cp "$SAO_FUENTES/scripts/iniciar-cliente-java.sh" scripts/
cp "$SAO_FUENTES/scripts/compilar-todo.sh" scripts/
cp "$SAO_FUENTES/scripts/empaquetar-fuentes-java.py" scripts/
cp "$SAO_FUENTES/docs/CLIENTE-JAVA.md" docs/
cp "$SAO_FUENTES/docs/PAQUETE-FUENTES-JAVA.md" docs/
cp "$SAO_FUENTES/docs/VALIDACION-PAQUETE-JAVA.md" docs/
mkdir -p docs/screenshots/java
cp "$SAO_FUENTES"/docs/screenshots/java/*.png docs/screenshots/java/
cp "$SAO_FUENTES/README-FUENTES-JAVA.md" ./

chmod +x scripts/compilar-cliente-java.sh scripts/iniciar-cliente-java.sh scripts/compilar-todo.sh scripts/empaquetar-fuentes-java.py
printf '\n/client-java/target/\n' >> .gitignore

git add .gitignore README-FUENTES-JAVA.md client-java/ \
  scripts/compilar-cliente-java.sh scripts/iniciar-cliente-java.sh \
  scripts/compilar-todo.sh scripts/empaquetar-fuentes-java.py \
  docs/CLIENTE-JAVA.md docs/PAQUETE-FUENTES-JAVA.md \
  docs/VALIDACION-PAQUETE-JAVA.md docs/screenshots/java/
git diff --cached --stat
git status --short
```

Revisa los archivos preparados. Después ejecuta cada comando cuando el anterior termine correctamente:

```bash
git commit -m "feat: incorpora cliente Java, recursos y documentación"
git pull --rebase origin main
git push origin main
```

El commit incluye archivos individuales. Si hay conflictos durante `pull`, resuélvelos antes del `push`. Esta receta copia los elementos del cliente a un clon actualizado; no sustituye los fuentes del servidor ni Angular.

## Alcance de validación

La entrega anterior documenta comprobaciones visuales Java/Angular en Linux con OpenGL por software. Este nuevo empaquetado comprueba fuentes, integridad y compilación del cliente desde una extracción nueva. No añade una validación visual de GPU física ni de Windows/macOS. Consulta el resultado actual en `VALIDACION-PAQUETE-JAVA.md`.
