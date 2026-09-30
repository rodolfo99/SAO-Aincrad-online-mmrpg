# Aincrad · Ecos del primer piso — v0.2.0

Proyecto semilla de un MMORPG 3D inspirado en el universo de **Sword Art Online**: servidor autoritativo **Java / Spring Boot**, cliente web **Angular + JavaScript / Three.js**, administración de mundo y NPC conversacionales con **Spring AI + Ollama**.

Es un prototipo fan con gráficos originales y dos pisos jugables. No es un juego oficial ni una recreación completa de los cien pisos, y no contiene recursos extraídos del anime ni de otros juegos.

Repositorio: [rodolfo99/SAO-Aircraft-online-mmrpg](https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg). El nombre del repositorio se conserva como `SAO-Aircraft-online-mmrpg`; el mundo del juego se llama **Aincrad**.

**Validación en Ubuntu 26.04:** partida completa realizada y confirmada por el usuario el 29 de septiembre de 2026. Consulta el registro de [validación manual](docs/VALIDACION.md#validación-manual-en-ubuntu-2604--confirmación-del-usuario).

## Obtener el proyecto desde GitHub

**Publicado en `main` el 29 de septiembre de 2026:** fuentes, ilustraciones, JAR v0.2.0, Angular compilado, scripts, configuración Docker y documentación. Para ejecutar esta entrega utiliza Java 17 o 21 y un navegador con WebGL 2. El arranque con los compilados incluidos no necesita Maven ni Node.js.

```bash
git clone https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg.git
cd SAO-Aircraft-online-mmrpg
chmod +x scripts/*.sh
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
./scripts/iniciar.sh
```

Abre <http://localhost:8081>; la administración está en <http://localhost:8081/admin>. En el primer inicio el script solicita la contraseña de root. El servidor Java sirve Angular, la API y el WebSocket en el mismo puerto; `npm start` se usa únicamente para desarrollar el cliente.

También puedes usar **Code → Download ZIP** en GitHub. Extrae el archivo, entra en la carpeta que contiene `scripts/`, `server/` y `client/`, y ejecuta el mismo arranque. Ese archivo sigue el contenido de `main`; el paquete `aincrad-seed-v0.2.0.zip` tiene una raíz llamada `aincrad-seed/`.

Guías: [arranque y puertos](docs/ARRANQUE-Y-PUERTOS.md) · [publicación, actualización y empaquetado](docs/PUBLICACION.md) · [operación y respaldos](docs/OPERACION.md) · [validación y límites](docs/VALIDACION.md).

## Inicio rápido en Linux

El paquete de entrega incluye el servidor compilado y el cliente construido. Para ejecutarlos basta **Java 17 o 21** y un navegador de escritorio con WebGL 2.

```bash
unzip aincrad-seed-v0.2.0.zip
cd aincrad-seed
chmod +x scripts/*.sh
./scripts/iniciar.sh
```

En el **primer inicio** el script solicita y confirma la contraseña de `root` (12–128 caracteres). No hay contraseña predeterminada. Si no hay terminal interactiva, proporciona `ROOT_PASSWORD` como variable de entorno. El servidor rechaza un primer inicio sin una contraseña válida.

- Juego: <http://localhost:8080>. Crea una cuenta e inicia sesión con usuario y contraseña antes de jugar.
- Administración: <http://localhost:8080/admin>, usuario **root**.
- Estado: <http://localhost:8080/api/health>
- Cierre ordenado: `Ctrl+C`.

La contraseña se almacena mediante PBKDF2-HMAC-SHA256, sal aleatoria y 210 000 iteraciones en `data/admin.json`. En arranques siguientes se utiliza la contraseña guardada; cambiar `ROOT_PASSWORD` no reemplaza la existente. Puedes cambiarla desde el panel. No compartas `data/`, respaldos ni `.env`.

Si actualizas una instalación anterior, consulta [PISOS-AMPLIADOS.md](docs/PISOS-AMPLIADOS.md) para ampliar su mundo conservando los personajes, la cuenta root y los ajustes. Una instalación nueva ya incluye los pisos grandes.

### Si el puerto 8080 está ocupado

Utiliza el comando de arranque con `PORT=8081` mostrado arriba y abre el navegador en 8081. `PORT` cambia la escucha de Spring Boot; `ALLOWED_ORIGINS` autoriza el origen del navegador y `APP_PUBLIC_URL` establece la URL de los enlaces de recuperación. Las tres opciones deben corresponder a tu instalación.

`scripts/iniciar.sh` no carga `.env` ni reenvía argumentos como `--server.port=8081`. Para imponer ese argumento debes ejecutar el JAR directamente como explica [ARRANQUE-Y-PUERTOS.md](docs/ARRANQUE-Y-PUERTOS.md). En Docker Compose usa `WEB_PORT=8081`: Java permanece en 8080 dentro del contenedor.

## Cuentas de jugador

El juego exige **registro e inicio de sesión con contraseña**. La cuenta guarda tus personajes en el servidor y permite recuperarlos desde otro navegador. Incluye selección de personajes, cierre de sesión, cambio de contraseña, recuperación por correo y recuperación administrativa desde root. El registro no inicia sesión automáticamente. Activa la utilidad integrada de [correo por IP](docs/CORREO.md) para enviar los enlaces de recuperación.

Si tienes personajes de la entrega anterior, inicia sesión y pulsa **Vincular personaje anterior** en el mismo navegador, o solicita a root que lo vincule a tu cuenta. Conserva `data/` y `world/` al actualizar. [Instrucciones y configuración de cuentas](docs/CUENTAS.md).

## Qué se puede jugar

- Dos pisos conectados, con cuatro veces la superficie inicial: Praderas del comienzo (184 m de diámetro) y Bosque de cristal (168 m). [Ampliación y actualización de partidas existentes](docs/PISOS-AMPLIADOS.md).
- Refugio, casas, vegetación, cristales, portales y colisiones generados desde una semilla reproducible.
- Cinco razas, ocho especialidades, 30 conjuntos de armadura y 30 juegos de armas con modelos 3D procedurales.
- Personajes con anatomía y rostros más detallados, pelo por mechones, materiales físicos, ropa por capas y articulación de brazos/piernas; editor con giro y primer plano del rostro. [Detalles visuales y límites](docs/PERSONAJES-3D.md).
- 48 talentos, incluidas 33 habilidades activas: daño, áreas, maldiciones, curaciones, mejoras e invocaciones.
- Herrería, sastrería, minería y leñador; 84 recetas, nueve materiales y recolección compartida. Bolsa con venta de recursos y mercado con 28 productos básicos de armas, ropa y armaduras, configurables por root.
- Herrería, sastrería y tienda visibles y configurables; patio con cuatro muñecos para practicar daño, curaciones y mejoras.
- PvP con ciudadanía, defensa propia, marca de asesino y protección del refugio, portales y patio.
- Movimiento con teclado o clic con rutas autoritativas por puntos intermedios; cámara giratoria; objetivo seleccionado; ataque normal y habilidad de clase.
- Cinco zonas de caza: slimes 1–3, perros 4–6, trasgos 7–10, orcos 11–15 y trolls 16–20; 26 criaturas nuevas con modelos 3D propios.
- Combate autoritativo, atributos y recompensas por nivel, persecución territorial, golpes anunciados, muerte y reaparición.
- Misión de tres jabalíes, EXP, niveles, col, pociones, mejora de arma y portal desbloqueable.
- Otros jugadores reales en el mismo piso, chat y guardado persistente de personajes.
- Ilustraciones generadas para el acceso, Lyra, las cinco razas, el bestiario y el árbol (nueve imágenes locales). Los modelos 3D son una interpretación procedural independiente de esas ilustraciones, no mallas riggeadas extraídas de ellas.

### Primer recorrido

1. Registra una cuenta e inicia sesión. Después elige aspecto, raza, clase, especialidad y nombre. Usa **◎ Entrenamiento → Ir al patio** para practicar; regresa al refugio para repartir talentos. Avanza unos pasos hasta **Lyra** y pulsa **E** para aceptar la misión.
2. Acércate a un jabalí, selecciónalo con un clic y pulsa **Espacio**. Usa **Q** para la técnica y **R** para curarte.
3. Derrota tres jabalíes y vuelve con Lyra (E). **Brann** abre la herrería con E. Puedes fabricar/mejorar piezas o elegir expresamente la mejora general de 50 col.
4. Ve al norte y derrota al **Centinela**. Su círculo naranja anuncia un golpe: sal de su alcance.
5. Acércate al cristal del portal norte y pulsa **F**. El segundo piso tiene portal de regreso al sur.

| Control | Acción |
|---|---|
| WASD / flechas | Mover respecto a la cámara |
| Clic en suelo | Buscar una ruta en el servidor y caminar, a la misma velocidad que con teclado |
| Clic en enemigo | Seleccionar objetivo |
| Arrastrar con botón derecho | Girar cámara |
| Rueda | Acercar/alejar cámara |
| Espacio / Q | Ataque / técnica, con enfriamiento |
| 1 / 2 / 3 / 4 | Habilidades activas aprendidas y asignadas |
| T | Habilidad racial |
| △ Zonas de monstruos | Consultar territorios por nivel y acercarse a ellos |
| ◎ Entrenamiento | Ir al patio, elegir muñeco y consultar DPS/curación |
| E / R / F | Interactuar / poción / portal |

El servidor calcula una ruta por puntos intermedios para rodear edificios, árboles, cristales y recursos usando las mismas colisiones de `WorldData.walkable`. WASD/flechas o la pérdida de foco cancelan la ruta. Un destino bloqueado, inaccesible en la malla o cuya búsqueda alcance el límite produce el `error` habitual y conserva el movimiento anterior. La malla de 1 m puede omitir pasos estrechos que requieran un desvío; cada búsqueda expande como máximo 8192 nodos. [Funcionamiento, pruebas y límites](docs/MOVIMIENTO-POR-CLIC.md).

**Para activar esta mejora recompila con `./scripts/compilar.sh` antes de arrancar.** El JAR precompilado de la entrega original v0.2.0 conserva su comportamiento anterior; esta revisión actualiza fuentes, pruebas y documentación.

## Administración del mundo

En `/admin` puedes cambiar semilla, nombres, bioma, radio y densidad de vegetación; crear pisos; editar aparición, portales, NPC y monstruos; y usar el editor JSON para cambios avanzados.

**Guardar y validar** comprueba límites, identificadores, destinos y puntos de aparición; guarda una copia de la definición anterior en `data/backups/`. **Aplicar y reconectar** carga el mundo guardado, conserva los personajes y solicita a los clientes reconectarse. Las posiciones que dejen de ser transitables se trasladan a la aparición de su piso. Si desaparece un piso se utiliza el primero disponible.

La edición de mundo no es un editor de mallas, escultor de terreno ni generador automático de cien pisos. Root configura especies, niveles, crecimiento, botín y territorios desde **Zonas de monstruos por nivel** y **Especies y crecimiento**, con vista previa 3D. Hay siete modelos: `slime`, `dog`, `goblin`, `orc`, `troll`, `boar` y `guardian`; puedes crear especies que los reutilicen. Los muñecos tienen configuración propia por piso. Consulta [docs/MONSTRUOS-ZONAS.md](docs/MONSTRUOS-ZONAS.md). Los roles de NPC son `guide`, `smith`, `tailor` y `merchant`. Amplía estos catálogos en Java y en el renderizador para nuevas mecánicas.

## Personajes, ropa y armaduras

Al crear personaje elige chico/chica, nombre, rostro, pelo, piel, clase, especialidad y raza, con vista previa 3D. La clase y especialidad determinan equipo y habilidades; las razas aportan geometría, atributos y una habilidad racial T. La elección persiste y se muestra a los demás jugadores.

Root puede restablecer contraseñas en **Usuarios registrados** y vincular personajes desconectados a sus cuentas. También dispone de **Personajes del mundo** para editar personajes conectados o desconectados y ajustar EXP, col y pociones con respaldo. El jugador tiene **Mi personaje** para editar únicamente su identidad/apariencia/clase/raza en el refugio, sin curarse ni cambiar EXP o monedas.

**Guerrero, mago y sanador** tienen ataques/habilidades diferentes y un árbol de talentos por clase. Al subir de nivel ganas puntos para atributos y talentos, que puedes repartir en el refugio. Root configura los árboles, costes, requisitos, efectos y puntos por nivel, y puede editar el reparto de cada personaje. Consulta [docs/PROGRESION.md](docs/PROGRESION.md).

Se incluyen **30 conjuntos de ropa/armadura** y **30 juegos de armas** por clase/especialidad y niveles 1, 5, 10; la ropa tiene variantes chico/chica. Root puede añadir, duplicar o eliminar conjuntos, cambiar colores y piezas, y configurar atributos de vida, daño, defensa, velocidad y crítico. También puede crear atributos personalizados con efectos reales o descriptivos. Consulta [docs/EQUIPO.md](docs/EQUIPO.md).

## Especialidades y práctica

| Clase | Especialidades / ramas | Equipo |
|---|---|---|
| Guerrero | Espadachín, asesino, tanque; cada uno con tres talentos activos encadenados | Espadas/mandobles y cuero/cota; dagas y cuero; espada/escudo y placas |
| Sanador | Maldiciones, mejoras y curación; individual y en área | Varas, báculos, escudos, orbes y tomos; túnica/cuero |
| Mago | Fuego, hielo, agua y tierra; invocaciones compartidas | Báculos de una/dos manos, varas, orbes y tomos; túnica |

La especialidad **asesino** es independiente de la marca criminal por PvP. Aprende habilidades en **Atributos y talentos** y asígnalas en **Habilidades activas**. Consulta [docs/ESPECIALIDADES.md](docs/ESPECIALIDADES.md).

El **patio de entrenamiento** está al sur del refugio del primer piso. Tiene tres muñecos ofensivos (uno acorazado) y un aliado herido. Registra daño, curación, DPS, críticos y mejoras; conserva alcance y recargas reales. No concede experiencia, monedas ni materiales. Root configura posición, PV, defensa, tipo y tiempos de reposición. Consulta [docs/ENTRENAMIENTO.md](docs/ENTRENAMIENTO.md).

## Oficios, recursos y PvP

En **Oficios y talleres**, visita a Brann para herrería y a Mira para sastrería. Fabrica, mejora y equipa tus propios objetos; recolecta vetas y árboles señalados mediante minería y leñador. Tessa vende materiales, pociones y equipo básico, y compra recursos de tu bolsa y piezas desequipadas. Las operaciones usan precios y recursos calculados en Java. Root configura productos, precios, recetas, atributos, edificios, materiales, nodos y niveles. Consulta [docs/OFICIOS.md](docs/OFICIOS.md).

**PvP y ciudadanía** permite activar ataques y seleccionar jugadores. Un asesinato injustificado resta inicialmente 25 puntos, deja marca de asesino y registra la baja de forma persistente. Hay defensa propia y zonas protegidas; salir durante combate deja el cuerpo expuesto durante el tiempo restante. Consulta [docs/RAZAS-PVP.md](docs/RAZAS-PVP.md).

## NPC con IA local

Consulta [docs/OLLAMA.md](docs/OLLAMA.md). En el mismo panel puedes configurar Ollama, consultar sus modelos, definir personalidad y modelo por NPC, probar conversaciones y guardar los ajustes. La IA está **desactivada por defecto** y no descarga modelos automáticamente. El juego funciona sin Ollama.

Las respuestas son diálogo; las reglas, recompensas y acciones del juego siguen ejecutándose en Java. El módulo no proporciona herramientas ejecutables al modelo. La memoria es temporal, separada por jugador y NPC.

## Compilar desde fuentes

Versiones fijadas del proyecto:

| Componente | Versión / requisito |
|---|---|
| Java | 17 para compilar; 17 o 21 para ejecutar |
| Maven | 3.9.9 recomendado, 3.6.3+ |
| Spring Boot | 3.5.15 |
| Spring AI | 1.1.8 |
| Node.js | 22.22.0 en `.nvmrc` y Docker; Angular acepta 20.19+, 22.12+ o 24.x dentro de esos majors |
| Angular / CLI | 21.2.4 |
| TypeScript | 5.9.3 |
| Three.js | 0.180.0 |
| Docker Compose | Plugin v2, opcional |
| Ollama en Compose | 0.17.7, opcional; modelo descargado por el usuario |

```bash
# Con un JDK, Maven y Node compatibles ya instalados:
./scripts/compilar.sh
./scripts/iniciar.sh
```

`package-lock.json` forma parte del proyecto: utiliza `npm ci`. No es necesario borrar datos, perfiles o configuraciones para compilar. Maven descarga las dependencias definidas en `server/pom.xml`; el primer build necesita Internet.

Desarrollo del cliente, en otra terminal con el servidor iniciado:

```bash
cd client
npm ci
npm start
# http://localhost:4200 — /api y /ws se redirigen al Java local
```

## Docker Compose

```bash
cp .env.example .env
# Ajusta puerto y orígenes si es necesario. No hay credencial prellenada.
./scripts/iniciar-docker.sh
```

El asistente pregunta la contraseña de inicialización y la pasa al contenedor por entorno. Alternativamente, define `ROOT_PASSWORD` de forma privada en `.env` y ejecuta `docker compose up --build -d`. Para volúmenes ya inicializados puede quedar vacía.

El servicio Java también sirve el Angular construido. Los volúmenes `game-data` y `game-world` mantienen perfiles, administrador, configuración IA y mundo editado. **No uses `docker compose down -v` para actualizar**, porque borra esos volúmenes.

```bash
docker compose logs -f game
docker compose down                 # conserva volúmenes
docker compose up --build -d        # actualiza código
```

Para LAN y Ollama en Docker, consulta [docs/OPERACION.md](docs/OPERACION.md) y [docs/OLLAMA.md](docs/OLLAMA.md).

## Pruebas

```bash
mvn -f server/pom.xml verify
npm --prefix client test
npm --prefix client run build
cd client
npx playwright install chromium
# Con servidor iniciado y contraseña root definida para la prueba:
AINCRAD_ADMIN_PASSWORD='tu-clave-de-pruebas' npm run test:browser
AINCRAD_ADMIN_PASSWORD='tu-clave-de-pruebas' npm run test:expansion
AINCRAD_ADMIN_PASSWORD='tu-clave-de-pruebas' npm run test:zones
```

Las pruebas de navegador editan perfiles de prueba y modifican/restauran el nombre del piso o el rango de una zona; usa un servidor y un directorio de datos aislados. El detalle de lo ejecutado en la entrega está en [docs/VALIDACION.md](docs/VALIDACION.md).

## Estructura y ampliación

- `server/src/main/java/dev/aincrad/`: simulación, red, persistencia, administrador y adaptador Spring AI.
- `client/src/app/world-renderer.js`: escena y animación Three.js en JavaScript.
- `client/src/app/`: interfaz Angular en TypeScript, compilada a JavaScript.
- `world/world.json`: definición editable y reproducible del mundo.
- `client/public/assets/`: ilustraciones originales incluidas.
- `scripts/`, `Dockerfile`, `compose*.yaml`, `.env.example`: construcción y operación.
- `docs/ARQUITECTURA.md`, `docs/MUNDO.md`: protocolo y puntos de extensión.

Esta semilla tiene límite de 32 jugadores conectados y un único proceso/zona lógica. **Ese límite no es una prueba de carga ni una promesa de escala MMORPG.** El registro, la autenticación de jugadores, la recuperación de cuentas y las rutas de clic dentro de cada piso están implementados. Quedan por desarrollar shards, comercio entre jugadores, subastas, grupos, navegación entre pisos, terreno avanzado, mallas GLTF y contenido extenso. La partida completa en Ubuntu 26.04 fue confirmada por el usuario; sigue pendiente validar una ejecución masiva en producción.

## Revisión visual del mundo

Ambos pisos incorporan árboles ramificados, edificios y talleres detallados, siete criaturas renovadas, minerales, muñecos y efectos elementales. Dentro de la partida, **⚙ Ajustes gráficos** cambia entre Ligera, Equilibrada y Alta. Root conserva el control de modelos, colores y configuración. Consulta [MUNDO-3D.md](docs/MUNDO-3D.md) para uso, alcance, compatibilidad y pruebas. El estilo sigue siendo 3D estilizado.

## Cuentas, correo y mercado

El acceso exige registro con usuario, correo y contraseña e inicio de sesión. Los personajes pertenecen a esa cuenta; se recuperan desde otros navegadores. Consulta [CUENTAS.md](docs/CUENTAS.md) para actualizar perfiles anteriores.

La recuperación usa una utilidad **Postfix incluida**, con envío directo por IP, sin servicio SMTP contratado ni dominio propio obligatorio. Activación, pruebas y diagnóstico: [CORREO.md](docs/CORREO.md). El puerto 25 de salida y la aceptación del destinatario condicionan la entrega; no se garantiza recepción sólo por activar el módulo.

La bolsa muestra recursos recolectados y permite vender cantidades al mercader. El mercado ofrece **28 productos básicos**, incluyendo armas, escudos, báculos, varas, ropa y armaduras. Root controla productos, disponibilidad y precios. Guía: [OFICIOS.md](docs/OFICIOS.md).
