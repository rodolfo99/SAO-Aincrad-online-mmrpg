# Movimiento por clic con rutas autoritativas

Revisión de fuentes de v0.2.0, 29 de septiembre de 2026. Un clic sobre el suelo solicita un destino; Java busca y sigue una ruta por puntos intermedios alrededor de los obstáculos del piso.

## Activar y probar en Ubuntu 26.04

Necesitas Java/JDK 17 o 21, Maven y una versión de Node compatible con `client/package.json` (por ejemplo Node 22.22.0). Detén el servidor antes de actualizar y conserva `data/` y tu definición `world/`. Desde la carpeta del repositorio actualizado:

```bash
chmod +x scripts/*.sh
./scripts/compilar.sh
PORT=8081 \
ALLOWED_ORIGINS=http://localhost:8081,http://127.0.0.1:8081 \
APP_PUBLIC_URL=http://localhost:8081 \
./scripts/iniciar.sh
```

El script construye Angular, ejecuta sus pruebas y `mvn verify`, y coloca el JAR nuevo en `release/`. Abre <http://localhost:8081>. El JAR precompilado de la entrega original v0.2.0 no incorpora esta revisión de fuentes; recompilar es necesario para activar las rutas.

En el primer piso, sitúate a un lado de una casa o taller y haz clic en suelo libre al otro lado. El personaje debe rodear el obstáculo. Puedes interrumpir el recorrido con WASD/flechas, cambiar el destino con otro clic o detenerlo al cambiar el foco de la ventana. Un clic dentro de un edificio muestra el error habitual. Un destino transitable encerrado por obstáculos también se rechaza; el clic rechazado conserva la intención anterior.

## Cálculo y colisiones

- `WorldData.walkable` conserva el disco de radio `floor.radius - 1` y los círculos de cada prop con `prop.radius + 0.4`. Edificios, vegetación, cristales y nodos de recursos proceden de los datos y de la generación existente.
- `walkableSegment` usa ese mismo modelo. Verifica ambos extremos y calcula la distancia mínima de cada prop al segmento completo, evitando cruzar un obstáculo aunque los extremos estén libres. El disco del piso es convexo, por lo que un segmento con extremos dentro permanece dentro.
- `WorldPathfinder` acepta directamente una línea libre. Si está bloqueada, usa A* con ocho vecinos en una malla de 1 m, costes euclídeos y heurística de distancia al destino. La posición exacta y el destino exacto se conectan a los nodos de su vecindad 3 × 3 únicamente mediante segmentos libres.
- La búsqueda expande como máximo 8192 nodos. La cola de prioridad resuelve empates de forma determinista. La ruta se simplifica con un recorrido lineal y solo permite atajos cuyo segmento completo siga libre.
- `GameWorld` guarda la cola únicamente en memoria. Cada tick reparte una sola distancia `player.speed() * dt` entre los puntos alcanzados, sin pausa ni velocidad adicional en las esquinas. La llegada conserva las coordenadas exactas del clic.
- Antes de avanzar, Java vuelve a comprobar el tramo contra el mundo actual. Si se ha bloqueado, vacía la cola y envía el `notice` existente: «La ruta dejó de estar disponible.»

## Compatibilidad y cancelación

| Intención o evento | Resultado |
|---|---|
| `move` con `x`, `z` finitos y ruta disponible | Reemplaza la cola; limpia la dirección residual del teclado |
| Destino bloqueado, fuera del piso, inaccesible en la malla o búsqueda agotada | `error` con texto; conserva la intención anterior |
| `input` válido, secuencia nueva y dirección distinta de cero | Cancela la cola y aplica WASD |
| `input` neutro | Mantiene el recorrido por clic |
| Secuencia antigua o duplicada | Se ignora, como antes |
| `stop` o pérdida de foco/visibilidad/cancelación de puntero | Vacía dirección y cola |
| Desconexión, reconexión, muerte PvE/PvP, aparición o portal | Vacía la cola; no reaparece una ruta anterior |

La entrada continua conserva la normalización diagonal, la caducidad a los 250 ms, el cálculo de velocidad y el deslizamiento por X/Z separado. El clic recorre los segmentos validados de su ruta.

El navegador conserva `{type:'move', x, z}`, `input` y `stop`; no calcula rutas ni decide posiciones. No hay un mensaje nuevo. Un campo extra `route`, `speed`, `hp`, `col` o `floor` en `move` no concede autoridad. Las respuestas `welcome`, `state`, `notice` y `error` conservan su formato. La selección de enemigos y la recolección siguen teniendo prioridad sobre el clic de suelo. Las rutas no se serializan en las instantáneas ni en los perfiles.

## Límites

La malla es una discretización del espacio transitable. Puede omitir desvíos por pasos estrechos o regiones sin conexión a sus nodos; una línea directa libre sigue siendo aceptada aunque el paso sea menor que 1 m. Un error de límite de búsqueda no demuestra que el destino sea físicamente imposible. No se cambia la geometría para forzar una ruta.

Se resuelven destinos dentro del piso actual. La navegación de monstruos mantiene su comportamiento existente. La ruta no contempla ocupación de otros personajes como obstáculos, porque `walkable` tampoco la contempla. No se añade navegación entre pisos ni una medida de capacidad multijugador.

## Pruebas reproducibles

Desde la raíz del repositorio:

```bash
mvn -B -f server/pom.xml verify
npm --prefix client ci --no-audit --no-fund
npm --prefix client test
npm --prefix client run build
```

Para ejecutar solo las pruebas del movimiento y su transporte:

```bash
mvn -B -f server/pom.xml \
  -Dtest=WorldPathfinderTest,ClickMovementTest,ServerIntegrationTest test
```

| Prueba | Cobertura |
|---|---|
| `WorldPathfinderTest` | Línea directa, puntos fraccionarios, casas y obstáculos repetidos, ambos pisos generados, recursos/árboles/cristales, segmentos que cruzan obstáculos con extremos libres, margen y borde, destino encerrado, presupuesto y determinismo |
| `ClickMovementTest` | Desvío real, llegada exacta, velocidad/modificadores y límite de tick, presupuesto único al cruzar una esquina, reemplazo/parada, entradas neutras/antiguas/inválidas, WASD, cambios de colisión, portal, persistencia/reconexión y muerte PvE/PvP |
| `ServerIntegrationTest` | Registro e inicio de sesión HTTP, WebSocket autenticado real, desvío alrededor del taller, velocidad observada, rechazo de mensajes inválidos y campos de autoridad, parada y secuencias antiguas |
| `client/tests/click-protocol.test.mjs` | Clic del renderer, formato exacto de `move`/`stop`, prioridad de selección/recolección y clic sin conexión; sin crear un contexto WebGL |

La integración usa puertos locales temporales y datos aislados. No necesita una instancia de Ollama para este recorrido. Los resultados y sus límites se registran en [VALIDACION.md](VALIDACION.md); una ejecución automatizada en este entorno no equivale a repetir la partida manual completa en Ubuntu 26.04.
