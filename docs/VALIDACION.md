# Validación de la entrega · 29 de septiembre de 2026 (UTC)

## Rutas autoritativas por clic · ejecución del 30 de septiembre de 2026 (UTC)

Se verificó la revisión de fuentes basada en `main` del commit `9e893fee3e581863ae95ab28e708761edf484192`, con **Java 17.0.20 y Node 24.19.0** en Linux:

- **`mvn -B -f server/pom.xml verify`: 160 pruebas, cero fallos, cero errores y cero omitidas.** Construcción y reempaquetado del JAR aprobados. Incluye 9 pruebas de `WorldPathfinderTest`, 14 de `ClickMovementTest` y las 6 de `ServerIntegrationTest`.
- **`npm --prefix client test` aprobado.** Se ejecutaron además sus cuatro archivos con `node --test --test-isolation=none --test-reporter=spec tests/*.test.mjs`: **16 casos, todos aprobados**, incluidos los cuatro nuevos del protocolo de clic/parada.
- **`npm --prefix client run build` aprobado.** El cliente conserva sus mensajes y el bundle resultante; la búsqueda vive en el servidor.
- **`git diff --check` aprobado.** Se actualiza `SHA256SUMS.txt` para las fuentes, pruebas y guías de esta revisión; el JAR precompilado de la entrega original permanece identificado como tal.

Se probaron los obstáculos reales de ambos pisos (edificios, árboles, cristales y recursos), segmentos con extremos libres que cruzan un círculo, destinos bloqueados y transitables pero encerrados, límites de coordenadas y búsqueda, determinismo y ausencia de caché obsoleta. La simulación verificó llegada exacta, presupuesto de velocidad único al cruzar una esquina, modificadores, entradas neutras/antiguas/inválidas, reemplazo y parada, deslizamiento WASD, cambios de colisión, portal, desconexión/reconexión, persistencia y muerte PvE/PvP.

La prueba de transporte registró una cuenta e inició sesión por HTTP, conectó un WebSocket real y recorrió ambos lados de la sastrería del primer piso. Observó el desvío y la velocidad en `state`, comprobó que una entrada neutra no cancelara la ruta, que campos extra no alteraran HP/col, que un destino bloqueado o un mensaje inválido produjera `error`, y que `stop` detuviera la cola incluso ante una secuencia antigua posterior.

Las pruebas que abren sockets se ejecutaron con acceso a puertos locales temporales; el intento inicial dentro del sandbox fallaba al abrirlos. El comando final `verify` sí ejecutó toda la batería correctamente. Estas comprobaciones no repiten una partida completa manual en Ubuntu 26.04 ni una prueba de carga. Para activar las rutas en una instalación, recompila el servidor: [MOVIMIENTO-POR-CLIC.md](MOVIMIENTO-POR-CLIC.md).

## Validación manual en Ubuntu 26.04 · confirmación del usuario

El usuario confirmó el **29 de septiembre de 2026** que ya realizó la **partida completa en Ubuntu 26.04**. Se registra como validación manual de la entrega v0.2.0 y se actualiza su estado de pendiente a realizada en la documentación y en el perfil de GitHub.

La fuente de este resultado es la confirmación del usuario. Las comprobaciones automatizadas se describen en los apartados siguientes; la validación de carga multijugador masiva y la entrega de correo a proveedores externos conservan su estado pendiente.

## Revisión de publicación y documentación · 29 de septiembre de 2026

Se verificaron los **255 archivos** del commit de publicación `944b3ab778733de18708a87381dcfe5952e0c806` comparando sus hashes de objetos Git con los bytes del ZIP de entrega; todos coincidieron. Esta revisión añade las guías de [arranque y puertos](ARRANQUE-Y-PUERTOS.md) y [publicación y actualización](PUBLICACION.md), actualiza los documentos relacionados y regenera `SHA256SUMS.txt` para los archivos distribuidos. El código, los compilados y la versión v0.2.0 se conservan.

Se comprobaron los enlaces locales de los documentos modificados y los enlaces de SAO en el README del perfil, la sintaxis Bash de sus bloques de comandos y el JSON del ejemplo de proxy.

Con **OpenJDK 17.0.20**, datos temporales y una copia aislada del mundo se comprobaron dos arranques del JAR publicado:

- `scripts/iniciar.sh` con `PORT=8081`: `/api/health` respondió `status=ok` y `version=0.2.0`; `/`, `/admin` y `/assets/lyra.png` respondieron HTTP 200.
- JAR directo con `PORT=18081` y `--server.port=18082`: las mismas comprobaciones respondieron correctamente en **18082**, confirmando el efecto del argumento explícito. Se usó otro puerto para mantener aislada la comprobación.

Ambos procesos se detuvieron de forma ordenada; los datos y las credenciales temporales no forman parte de la publicación. Estas comprobaciones verifican el arranque, las rutas y una imagen servida; no equivalen a una sesión de juego ni a una prueba visual de navegador.

No se repitieron Maven, la batería Node/Python ni el build Angular por este cambio documental. Las cifras que siguen pertenecen a la validación anterior del paquete. Docker Compose, el proxy Angular en ejecución, la carga masiva y la entrega de correo externo no se ejecutaron en esta revisión. Las instrucciones de Compose/proxy se contrastaron con los archivos publicados. La partida completa en Ubuntu 26.04 confirmada posteriormente por el usuario está registrada en el apartado de validación manual.

## Cuentas, recuperación por correo y mercado · validación anterior del paquete

Se aprobaron **136 pruebas Maven**, **12 pruebas Node**, **7 pruebas Python** y la compilación Angular de producción. Las comprobaciones nuevas cubren autenticación obligatoria, propiedad y conservación de personajes, recuperación de contraseñas, utilidad de correo, venta de recursos y compra de equipo. No se incluyen cuentas ni partidas de prueba en la entrega.

`client/tests/accounts-browser.mjs` aprobó **ocho flujos** con Angular y Spring reales: registro/inicio de sesión separados, vinculación de un personaje generado por el JAR de la entrega anterior conservando experiencia/monedas/pociones, creación y movimiento, cierre de sesión, recuperación de personajes desde otro navegador, cambios y restablecimientos de contraseña, recuperación por enlace recibido mediante un receptor SMTP TCP local y prueba de correo root. Incluye el formulario de registro a 390 px de ancho. El receptor local verifica mensajes reales, pero no representa entrega a un proveedor externo. Informe: `accounts-browser-result.json`; capturas `33-` a `40-`.

`client/tests/market-browser.mjs` aprobó **seis flujos**: minería con desplazamiento real, bolsa y restricción de venta lejos del mercader, ventas parciales/completas y rechazo de cantidades excesivas, compra de arma y armadura compatibles, equipamiento en el refugio, edición de precios mediante root y conservación de la bolsa/equipo al aplicar el mundo y volver a entrar. El mundo desechable de la prueba usa precios de equipo de 2 col para financiar las compras con recursos recolectados; la entrega conserva sus precios normales y sus **28 productos**. Informe: `market-browser-result.json`; capturas `41-` a `43-`.

Ambas pruebas utilizan Chrome Headless Shell 145.0.7632.6 con SwiftShader, una página activa cada vez, sin errores JavaScript de página o consola. Las capturas del registro, restablecimiento, mercado y administración se revisaron visualmente.

Se repitieron también las **cinco comprobaciones de pisos ampliados** con una cuenta registrada: conectividad del mundo, edición root, desplazamiento más allá del límite antiguo, transición por ambos portales y conservación del personaje/minimapas. Pasaron sin errores de página o consola. El informe `expanded-floors-result.json` y las capturas `29-` a `32-` corresponden a esta ejecución con autenticación.

La utilidad Postfix se entrega configurada para envío directo por IP, con red privada y cola persistente. No había Docker Engine ni Postfix ejecutable en este entorno, por lo que no se arrancó ese contenedor. El intento autorizado a Hotmail no pudo establecer la conexión saliente al puerto 25 (`Network is unreachable`); **no se envió un correo externo**. Los mensajes SMTP de integración sólo llegaron al receptor local. Consulta [CORREO.md](CORREO.md) para activación y diagnóstico en el equipo de destino.

## Ampliación de los pisos · revisión anterior

Se repitieron **Maven `verify` (102 pruebas), Node (12 pruebas) y la compilación Angular de producción**, todos aprobados. Se añadieron **tres pruebas Python del actualizador**: conservación de reglas/copia exacta/idempotencia, rechazo completo de geometría personalizada y detección de IDs nuevos en conflicto. Se corrigió una prueba de portal que fijaba las coordenadas antiguas; ahora consulta la definición del piso. El código de producción Java no cambia.

`npm ci` se comprobó desde cero. El bloqueo anterior apuntaba a un paquete `htmlparser2@10.2.0` que devolvía 404; se fijó `htmlparser2@10.0.0`, compatible con el rango de Beasties, y se regeneró el archivo de bloqueo. No requiere cambios manuales al instalar.

El JAR de entrega cargó y validó el nuevo mundo con **255/225 elementos de vegetación y 12/10 recursos**. Sobre sus colisiones públicas se calculó conectividad a portales, NPC, cinco territorios y 22 nodos con una cuadrícula de 1 m y un margen de obstáculos de 0,9 m (el servidor utiliza 0,4 m). El corredor central está libre. Esta comprobación es de conectividad; no añade búsqueda de rutas al cliente.

`client/tests/expanded-floors-browser.mjs` aprobó **cinco comprobaciones** en Angular y Spring reales: conectividad; dimensiones root/guardado/recarga; movimiento hasta Z=−52 y rechazo de un destino fuera del radio nuevo; portal real en Z=−76, llegada a Z=58 y minimapas completos; regreso por Z=63 conservando el personaje. Se utiliza una única página activa con **Chrome Headless Shell 145.0.7632.6 / SwiftShader**. Sólo se desactiva el requisito de jefe en la copia aislada del mundo de prueba: el archivo de la entrega mantiene ese requisito. Sin errores de página ni de consola. Informe: `expanded-floors-result.json`; capturas inspeccionadas `29-` a `32-`.

Se verificó además la ampliación del JSON original de la entrega anterior: produce las mismas dimensiones y entidades de la nueva semilla y conserva los campos restantes. Las capturas e informes de las revisiones visuales anteriores que siguen a continuación corresponden al mapa compacto. Sus métricas de memoria no se presentan como medidas del mapa ampliado. No se han medido FPS ni carga multijugador en hardware del usuario.

## Ejecución automática

- **Maven `verify`: 136 pruebas, 0 fallos, 0 errores.** Java 17.0.20, Maven 3.9.9, Spring Boot 3.5.15 y Spring AI 1.1.8.
- **Cliente: 12 pruebas de Node aprobadas.** Node 24.19.0 y npm 11.9.0 en el entorno de construcción.
- **Python: 7 pruebas aprobadas.** Actualizador de pisos y configuración del correo por IP, conservación de variables/copias, validación de direcciones/URL y restricción del relay privado.
- **Angular: compilación de producción completada.** Código Angular/TypeScript y renderizador JavaScript incluidos, con dependencias fijadas en `package-lock.json`.
- **ZIP extraído y ejecutado correctamente**, incluso desde una ruta con espacios. Se comprobaron todos sus hashes, la inicialización de root, `/api/health`, Angular y las nueve imágenes locales, las dimensiones ampliadas y los 28 productos. Se verificaron el bloqueo de WebSocket anónimo y el registro/inicio/cierre de sesión con correo. El archivo no contiene partidas, contraseñas de prueba ni dependencias de desarrollo instaladas.
- YAML de Compose parseado y sintaxis de todos los scripts Bash comprobada.

Las pruebas del servidor utilizan directorios temporales, un servidor Spring real, dos WebSockets reales y un proveedor HTTP local que implementa el contrato necesario de Ollama. No escriben sobre una partida existente.

## Cobertura del servidor

| Área | Comprobado |
|---|---|
| Mundo | Semilla reproducible, semilla distinta, destinos de portal, aparición bloqueada y catálogos válidos |
| Movimiento | Diagonal normalizada, caducidad de entrada, rechazo de secuencias antiguas, velocidad de clic, imposibilidad de enviar posición/estadísticas arbitrarias |
| Zonas y especies | 26 apariciones deterministas; niveles, armadura, daño y recompensas escalados; botín sin duplicar; reaparición; persecución territorial y ataques a distancia; retirada y recuperación de bloqueo; zonas seguras, densidad e IDs; especies root personalizadas |
| Combate | Alcance, enfriamiento, muerte/reaparición de criatura, recompensas, misión una vez, restricciones del portal |
| Cuentas | Contraseña con sal, registro/login separados, cookies, sesiones caducadas/revocadas, límites de intentos, origen y WebSocket autenticado; rechazo de personajes ajenos y del acceso con claves antiguas |
| Recuperación | Correo único, respuestas sin revelar cuentas, tokens con hash, caducidad, consumo único incluso concurrente, invalidación por cambios/root, revocación de sesiones, enlaces desde URL configurada y mensajes SMTP locales reales |
| Personajes | Guardado y recuperación por cuenta, migración/vinculación de perfiles anteriores, rechazo de doble sesión, aspecto y estadísticas de clase, edición propia en refugio y root sobre perfiles desconectados |
| Equipo | Selección por clase/género/nivel, atributos con efecto real o descriptivo, límites y conjunto inicial obligatorio |
| Progresión | Presupuesto de puntos, requisitos, rangos enteros, rechazo de árboles ajenos, cambio de clase, redistribución, persistencia, subida de nivel, ciclos y devolución de repartos incompatibles |
| Clases | Daño real a distancia del mago, curación propia y de aliados del sanador, enfriamiento y participación cooperativa |
| Razas y armas | Cinco razas, raciales y recargas, dos manos/secundaria, selección por nivel/especialidad y persistencia |
| Especialidades | Ocho especialidades, 33 habilidades, restricciones de ramas/equipo, cura individual/área, maldiciones, provocación, invocaciones y barra persistente |
| PvP | Alcance, zonas seguras, defensa propia, sanción única, recibos, cuerpo desconectado, reinicio, redención e indulto root |
| Oficios | Costes, propiedad, inventario, equipo, mejoras, compra/venta, requisitos, colisiones de talleres y recetas válidas |
| Mercado y bolsa | 28 productos, compatibilidad de equipo, piezas individuales, precio calculado por servidor, venta parcial/completa de recursos, rechazo de exceso/combatir/lejanía, límites de monedas, reventa una sola vez, persistencia y reversión ante fallo de escritura |
| Recolección | Agotamiento compartido, XP independiente, materiales raros, recuperación por recibos y rechazo fuera de alcance |
| Entrenamiento | Daño letal sin recompensa, armadura, curas/mejoras al aliado, área a tres muñecos, invocación, DPS y reinicio, zona segura |
| Migración | Mundos v0.1.0 y esquema 6 originales a esquema 7, copia previa, preservación de mapas, razas antiguas y botín |
| Root | Inicialización sin clave predeterminada, contraseña persistida, hash con sal, sesiones, rate limit, origen, validación/backup/recarga, rutas protegidas y ausencia de secretos de jugador en API |
| Spring AI | API Ollama, mensajes de sistema/contexto, opciones del modelo, error de proveedor, timeout limitado, fallback, persistencia y validación de enlaces NPC |

## Revisión integral del mundo 3D

Esta revisión repite la compilación de producción y las **12 pruebas Node**: las nueve anteriores y tres nuevas que cubren geometría finita/presupuesto/articulación del bestiario, paleta root, agotamiento de recursos, diferencias de árboles, invocaciones y límite/liberación de efectos bajo ráfagas. El servidor Java no ha cambiado.

`client/tests/world-visual.mjs` aprueba cinco comprobaciones: ambos pisos/talleres/portal; selección por clic del muñeco y la veta correctos; tres calidades y seis cambios de piso; siete especies; talleres, recursos y seis elementos. Se usan los props públicos del servidor y una escena de inspección con las mismas fábricas del juego. Tras los cambios de piso, las geometrías/texturas residentes vuelven exactamente a **804/18**, igual que al comenzar la comprobación. No hubo errores WebGL ni de consola. El registro `docs/world-visual-result.json` incluye métricas de cada vista y los resultados del picking.

Las capturas se inspeccionaron para corregir el material del suelo, la paleta del Centinela, los ojos de criaturas y los rótulos. Se corrigió la selección de partes ocultas de recursos, la liberación del mapa de sombras al reconstruir la escena y los círculos que se mostraban en enemigos sin seleccionar.

`client/tests/world-browser.mjs` aprueba **cinco flujos con Angular y Spring reales**: entrada/WASD/Lyra; cambios y persistencia de calidad; selección, daño y curación en el patio; vista y edición root del Centinela con guardado/recarga; y transición al piso de cristal conservando el perfil. El mundo de prueba acerca el portal al refugio mediante root y quita el requisito de jefe exclusivamente en esa copia aislada. No modifica `world/world.json` de la entrega. No hubo errores de página ni de consola. Registro: `docs/world-browser-result.json`; capturas `24-` a `28-`.

Los cinco flujos anteriores de inspección gráfica y estos cinco de integración se ejecutan con una página activa cada vez en SwiftShader. Las métricas no son FPS de hardware ni una prueba de carga multijugador.

## Revisión visual de personajes

La compilación de producción y las nueve pruebas Node se han repetido para esta revisión. Tres pruebas nuevas comprueban las mallas y articulaciones de cinco razas y dos géneros, los 30 conjuntos de armas y la selección explícita de equipo en la vista root. La geometría de cada combinación comprobada queda por debajo de 90 000 triángulos. No se han cambiado archivos Java ni se presentan sus 102 pruebas como repetidas por un cambio exclusivo del cliente.

`client/tests/avatar-visual.mjs` comprueba diez combinaciones de raza/género con tres clases y niveles 1/5/10, Lyra, giro con teclado, encuadre estrecho y doce reconstrucciones sucesivas sin crecimiento del número de geometrías o texturas residentes. No hubo errores de WebGL ni de consola. El registro es `docs/avatar-visual-result.json`; incluye triángulos y llamadas de dibujo del modelo visible. Se inspeccionaron las capturas de rostros, enano con armadura, elfa sanadora y draconiana con alas.

En SwiftShader, la prueba que mantuvo juego y administración abiertos a la vez agotó el tiempo de espera de root. Por ello, el flujo administrativo se comprueba secuencialmente, cerrando la escena del juego y volviendo a entrar al finalizar; no se presenta como validada la carga gráfica simultánea de ambas pestañas.

Se aprobaron también **seis flujos de Angular con servidor real** en `client/tests/avatar-browser.mjs`: creación y giro; movimiento y diálogo con Lyra; autoedición; edición root de raza/género/clase/especialidad/nivel; vista del conjunto concreto con color/casco; y reentrada con el perfil actualizado. El juego se cierra durante la fase root de esta prueba. No hubo errores de página ni de consola. Registro: `docs/avatar-browser-result.json`. El ajuste final del encuadre de retrato se verifica por separado en `docs/avatar-portrait-result.json`.

Estas métricas describen vistas individuales. No equivalen a una prueba de FPS de una ciudad llena de jugadores. Se utiliza Chromium 153.0.8010.0 con SwiftShader, un renderizador por software. Los modelos continúan siendo estilizados, con más detalle y materiales físicos; no se certifica fotorrealismo.

## Navegador

En la ampliación de **esquema 7 se aprobaron seis flujos** de `client/tests/zones-browser.mjs`: carga de zonas/especies, panel de ambos pisos, edición root y reconexión con niveles nuevos, cinco modelos 3D, nivel/vida del objetivo y entrada física en la marisma. El desplazamiento usa intenciones normales y obstáculos públicos; no teletransporta al personaje. El servidor y la partida de prueba están aislados. Se comprueba que el nombre de la zona no quede tapado por la ficha del objetivo.

Se corrigió la carga inicial de root para completar la lectura del mundo antes de montar sus editores. Se verificó también una recarga con la cookie administrativa existente. Las cinco vistas previas se inspeccionaron; el encuadre se adapta a la geometría y al tamaño del canvas para mostrar el troll completo. Las pantallas de zonas, marisma y editor se revisaron visualmente. No hubo errores de página ni errores JavaScript de consola en estas comprobaciones. Registros: `docs/zones-browser-result.json` y `docs/bestiary-preview-result.json`.


Como antecedente de la entrega de esquema 6, **19 flujos aprobados en Chromium 153.0.8010.0** (11 de base + 8 de ampliación), con dos sesiones independientes para movimiento/chat y una sesión root. No se registraron errores JavaScript de página. Durante el arranque de la segunda sesión se observó un intento WebSocket cerrado antes de establecer conexión; la reconexión automática completó el flujo. Se verificó también la recuperación del nivel y los talentos tras recargar la página.

Las pruebas reproducibles están en `client/tests/browser.mjs` y `client/tests/expansion-browser.mjs`. La ampliación comprueba draconiano/tanque, cambio de clase y armas, fabricación/mejora/equipo, compra/venta, minería, árbol de fuego/invocación, patio con área/DPS/reinicio y recuperación de progreso. Usa las facultades root para preparar nivel, monedas y cobre de prueba; todos los movimientos son intenciones normales, con rutas de prueba calculadas alrededor de los obstáculos públicos. No se teletransporta ni se desactivan criaturas. Necesita un servidor aislado y `AINCRAD_ADMIN_PASSWORD`; usa Playwright/Chromium con WebGL. Las capturas de las pantallas comprobadas se incluyen en `docs/screenshots/`.

Estos 19 flujos corresponden a la validación previa; no se presentan como repetidos en esta ampliación. El registro de aquella ejecución, con la versión del navegador y pasos aprobados, se incluye en `docs/browser-result.json` y `docs/expansion-browser-result.json`. Se inspeccionaron capturas de creación, talleres y entrenamiento; se ajustaron rótulos largos y contraste de los indicadores.

## Límites de la verificación automatizada

- No se ejecutó un modelo LLM real de Ollama. Se probó el adaptador Spring AI contra respuestas HTTP controladas; el usuario debe instalar/descargar su modelo para validar calidad, latencia y consumo de memoria.
- No había Docker Engine disponible: no se construyeron ni arrancaron los contenedores, y no se probó GPU/NVIDIA. Se verificaron sus archivos de configuración; su ejecución queda pendiente en el equipo de destino.
- En el entorno automatizado no se probaron Java 21, dispositivos móviles reales, Safari ni hardware GPU del usuario. El ancho móvil de 390 px se comprobó en Chromium de escritorio. La partida completa en Ubuntu 26.04 fue confirmada por el usuario y se registra como validación manual.
- La prueba con dos jugadores verifica sincronización funcional. No es una prueba de carga de 32 jugadores ni una validación de escala masiva.
- El mapa, los monstruos y las reglas se probaron por partes; no se presenta como terminada una campaña de cien pisos.
