# Aincrad Java Desktop · 0.1.0

Cliente de escritorio Java 17 para el mismo Aincrad que utiliza Angular. Incluye interfaz JavaFX y mundo 3D nativo jMonkeyEngine/OpenGL, sin navegador, WebView ni servidor integrado. Utiliza las cuentas, personajes, API y WebSocket existentes del servidor v0.2.0.

## Ejecutar el paquete generado localmente

El repositorio contiene los fuentes, recursos, scripts y documentación como archivos individuales. Para ejecutarlo desde esta copia, sigue primero «Compilar desde el repositorio» más abajo. Maven también genera un paquete para copiar el cliente ya compilado a otro equipo; ese ZIP no se publica en el repositorio.

El paquete generado está compilado y comprobado en **Linux x86_64**. Necesitas Java 17 o 21, sesión gráfica y un controlador que admita OpenGL 3.3. No necesitas Maven, Node.js ni Angular para ejecutar ese paquete.

```bash
unzip client-java/target/aincrad-client-java-0.1.0-desktop.zip
cd aincrad-client-java-0.1.0
./iniciar-cliente.sh http://localhost:8081
```

El servidor debe estar arrancado por separado. También puedes indicar su dirección en la pantalla de acceso, con `--server=https://tu-servidor.example` o mediante `SAO_SERVER_URL`. Por defecto se usa `http://localhost:8081`. Introduce tu misma cuenta de Angular; registrar una cuenta exige después iniciar sesión.

```bash
SAO_SERVER_URL=http://192.168.1.20:8081 ./iniciar-cliente.sh
```

## Compilar desde el repositorio

Necesitas un JDK 17 o 21 y Maven 3.9.9 o posterior. El arte ya está incluido; la compilación Java no ejecuta npm.

```bash
./scripts/compilar-cliente-java.sh
./scripts/iniciar-cliente-java.sh http://localhost:8081
```

Equivalente: `mvn -B -f client-java/pom.xml verify`. Crea el JAR, `target/lib/` y `target/aincrad-client-java-0.1.0-desktop.zip`. Las pruebas de contrato arrancan el JAR original del servidor en puertos temporales y con datos separados. No utilizan tu partida.

En Windows compila en ese sistema con `mvn -f client-java/pom.xml verify` y ejecuta `client-java\iniciar-cliente.bat --server=http://localhost:8081`. En macOS compila allí y utiliza el script `.sh`. Maven selecciona los binarios JavaFX de la plataforma que compila; **el ZIP Linux no es un paquete Windows/macOS**. Esos sistemas no se comprobaron gráficamente en esta entrega.

## Funciones

- Acceso, registro, recuperación, restablecimiento de contraseña, cambio de correo/contraseña, cierre de sesión y vinculación de personajes anteriores mediante su clave.
- Selección, creación y edición de personajes; cinco razas, tres rostros, dos géneros, clases, especialidades y colores, con vista 3D de cuerpo y rostro.
- Ambos pisos, movimiento con teclado/clic/mapa, objetivos, ataques, técnica de clase, habilidad racial, pociones, misión de Lyra, Centinela, portal y reaparición.
- Chat del piso, conversación con NPC y Ollama cuando el servidor la habilita.
- Inventario, armas automáticas o elegidas, piezas artesanales, mejoras, mercado, materiales, recolección y oficios.
- Atributos, talentos, habilidades aprendidas y cuatro ranuras; entrenamiento, DPS/HPS, zonas, PvP, ciudadanía y redención.
- Administración root nativa: mundo y todos sus catálogos, personajes, cuentas, equipo, ciudadanía, oficios, IA/Ollama, correo y contraseña root.

## Imagen y controles

Las 562 variantes de arte se exportan de las mallas originales del repositorio. El renderizado añade normales de superficie para tela, cuero, madera, piedra y metal, reflejos metálicos, sombras PCF, oclusión ambiental, bloom y antialiasing. Árboles con hojas individuales, tejas, caras, dedos, equipo, criaturas e invocaciones conservan su geometría detallada.

| Calidad | Imagen |
| --- | --- |
| Ligera | Sin sombras ni SSAO; resolución limitada a 1000 × 625; sin hierba decorativa adicional. |
| Equilibrada | Sombras 1024, bloom y FXAA; hasta 1600 × 1000. |
| Alta | Sombras 2048, SSAO, bloom, FXAA y hierba adicional; hasta 2560 × 1600 y escala de pantalla de alta densidad. |

La proporción de la ventana se conserva. El motor intenta presentar hasta 30 imágenes por segundo; el rendimiento depende del equipo. La calidad se conserva entre sesiones, las contraseñas y cookies no se guardan en disco.

WASD/flechas mueve según la cámara; clic camina/selecciona; botón derecho o central arrastrado gira e inclina; rueda acerca. Espacio ataca, Q usa la técnica, T la habilidad racial, R la poción, E conversa, F activa el portal y 1–4 usa habilidades. Esc para/cierra paneles; F11 cambia a pantalla completa. Cambiar de ventana detiene la entrada de teclado.

## Compatibilidad y diagnóstico

El servidor decide posiciones, colisiones, velocidad, recompensas, recargas y permisos. El arte adicional no cambia obstáculos ni rutas. La revisión actual de los fuentes del servidor calcula rutas por clic; Java utiliza esa mejora mediante el mismo comando `move`. El JAR original incluido conserva su comportamiento anterior. Angular y Java pueden participar simultáneamente con personajes distintos, incluso de la misma cuenta. Una misma identidad sigue sujeta a las reglas de sesión del servidor.

El cliente envía `Origin` igual al origen del servidor configurado, conserva la cookie `aincrad_player` en memoria y utiliza `/ws`. Si el servidor rechaza el origen, configura `ALLOWED_ORIGINS` con esa dirección. En un servidor HTTPS utiliza también HTTPS en Java; así se conserva la cookie segura y se usa WSS.

Si falla OpenGL, comprueba el controlador y que existe sesión gráfica. En Ubuntu/Debian las bibliotecas habituales son `libgl1`, `libgtk-3-0`/`libgtk-3-0t64`, `libxtst6`, `libxi6` y `libxrender1`. Prueba Ligera si hay poca fluidez. En Linux ARM o macOS ARM necesitas compilar y verificar los binarios nativos correspondientes; no reutilices el ZIP x86_64.

La guía ampliada, matriz de funciones y pruebas están en [`docs/CLIENTE-JAVA.md`](../docs/CLIENTE-JAVA.md) del repositorio. Consulte también `NOTICE.md` y `licenses/` para las dependencias redistribuidas.
