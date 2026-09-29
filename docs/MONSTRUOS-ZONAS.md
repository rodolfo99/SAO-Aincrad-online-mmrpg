# Zonas de monstruos por nivel

La semilla incluye cinco territorios de caza y 26 criaturas nuevas. Se mantienen los ocho jabalíes/centinelas de las apariciones anteriores y los cuatro muñecos de prácticas. Los niveles indican dificultad recomendada; no impiden entrar a un personaje de otro nivel.

| Piso | Zona | Especie | Nivel | Población | Reaparición | Centro X, Z | Radio |
|---|---|---|---|---:|---:|---|---:|
| 1 | Marisma de los slimes | Slime verde | 1–3 | 6 | 16 s | 20, 8 | 7 m |
| 1 | Hondonada de los perros | Perro salvaje | 4–6 | 5 | 20 s | −24, −6 | 7 m |
| 1 | Campamento trasgo | Trasgo merodeador | 7–10 | 5 | 24 s | 22, −18 | 7 m |
| 2 | Avanzada de los orcos | Orco de hierro | 11–15 | 5 | 30 s | −20, −10 | 9 m |
| 2 | Quebrada de los trolls | Troll de piedra | 16–20 | 5 | 40 s | 19, −17 | 9 m |

En el segundo piso hay diez criaturas de zona; en el primero, dieciséis. El acceso al piso 2 conserva el portal y el desbloqueo del Centinela. La población es compartida por todos los jugadores del piso.

## Localizar y combatir

Abre **△ Zonas de monstruos** para consultar ambos pisos, rangos, población y tiempo de reaparición. El panel indica si estás dentro y cuántas criaturas de la zona siguen vivas en el piso actual. **Acercarse** envía una intención normal de caminar hacia un punto transitable junto al borde interior. Como el resto del movimiento por clic, no calcula rutas completas: rodea árboles o casas si bloquean el camino.

El minimapa señala los territorios con círculos. El terreno muestra su perímetro y un rótulo cercano. Al entrar aparece el nombre y rango de nivel en la cabecera; seleccionar una criatura muestra su nivel y vida. Slimes, perros, trasgos, orcos y trolls tienen geometrías Three.js propias, reutilizadas en la vista previa del editor root. El slime rebota, el perro tiene cuatro patas y cola animada, y los humanoides llevan dagas, hachas o mazas según su modelo.

Las criaturas detectan jugadores cercanos y persiguen a sus atacantes dentro del territorio y del límite de persecución de su especie. Los ataques a distancia generan amenaza aunque lleguen desde fuera del radio de detección. Al perder un objetivo válido, la criatura vuelve a su aparición: durante ese regreso no recibe daño, limpia amenaza/efectos y recupera toda la vida al llegar. Si queda bloqueada, se recoloca en su punto tras ocho segundos de retirada. No entrega recompensas por retirarse. Los trolls anuncian su golpe antes de ejecutarlo.

Al morir, la criatura desaparece de la instantánea hasta completar su temporizador de reaparición. Conserva ID, nivel y punto de origen. Reiniciar el servidor o aplicar una nueva definición reconstruye las poblaciones completas; HP y temporizadores de criaturas no son persistentes.

## Dificultad y recompensas

Cada especie define valores base y crecimiento. Para nivel N:

`valor(N) = base + crecimiento × (N − 1)`

Java calcula vida, daño, defensa, EXP y col. Daño y defensa se redondean al entero más cercano. Los límites finales son 200 000 PV, 1–1 000 daño, 0–200 defensa, 10 000 EXP y 100 000 col por criatura. La defensa reduce el daño recibido con mínimo de un punto. Se siguen aplicando límites de perfil, equipo, alcance y recargas; ningún campo enviado por el jugador otorga recompensas ni fija estadísticas.

| Especie | PV en su rango inicial | EXP por derrota | Col por derrota | Materiales por derrota |
|---|---|---|---|---|
| Slime | 32–56 | 20–36 | 8–12 | 1 fibra, 1 cristal |
| Perro | 88–124 | 54–74 | 17–23 | 2 cueros |
| Trasgo | 172–238 | 100–136 | 34–46 | 2 cobres, 1 pino |
| Orco | 345–457 | 175–231 | 54–70 | 3 hierros, 2 cueros |
| Troll | 700–860 | 320–392 | 110–134 | 1 mithril, 2 cristales |

La recompensa sigue la participación cooperativa existente: la recibe cada participante vivo, conectado, en el mismo piso y a un máximo de 22 m al producirse la derrota. Curar a un participante cercano también genera participación. Los materiales son cantidades fijas por especie; EXP y col crecen con el nivel. El botín se almacena únicamente en `crafting.monsterDrops`, sin una segunda tabla de premios.

Solo la especie `boar` avanza la misión de jabalíes. Inicialmente solo el Centinela tiene `unlockPortal: true`. Los cinco enemigos nuevos no activan ese desbloqueo ni generan sanciones PvP.

## Configurar desde root

1. En `/admin`, elige un piso y abre **Zonas de monstruos por nivel**. Puedes añadir/eliminar zonas y editar especie, nombre, nivel mínimo/máximo, centro, radio, población y reaparición.
2. En **Especies y crecimiento**, elige o añade una especie. Ajusta nombre, modelo, color, valores base y crecimiento, velocidad, alcance, detección, límite de persecución, intervalo de ataque, aviso previo y recompensas. La vista previa usa el mismo modelo que la partida.
3. Define **Materiales por derrota** y, si corresponde, el desbloqueo de portal. La reaparición de la zona prevalece sobre la individual de la especie.
4. Para apariciones manuales, elige la especie y el nivel en la tabla de monstruos del piso. Su nombre puede ser particular y usan la reaparición de la especie.
5. Pulsa **Guardar y validar** y después **Aplicar y reconectar**. Se respalda la definición anterior y se conservan personajes; la población del mundo se reconstruye.

Una especie nueva puede reutilizar cualquiera de los siete modelos (`slime`, `dog`, `goblin`, `orc`, `troll`, `boar`, `guardian`) con otro color y reglas. Un octavo tipo de geometría requiere ampliar el renderizador y el catálogo de modelos de Java. Antes de eliminar una especie retira sus zonas y apariciones; el servidor rechaza referencias inexistentes.

## Definición y límites

`monsterSpecies` es el catálogo global; cada `floors[].monsterZones` define sus áreas. Los IDs de zona admiten 1–48 caracteres ASCII alfanuméricos, guion o guion bajo y son únicos en todo el mundo. Los IDs de criaturas se derivan como `<zona>-1`, `<zona>-2`, etc.; también se comprueban contra el resto de entidades.

Se admiten 1–50 especies, hasta 12 zonas por piso, radio de 3–18 m, nivel de 1–1 000, 1–30 criaturas por zona y como máximo 200 criaturas hostiles por piso sumando apariciones manuales. Los muñecos conservan sus propios límites. La reaparición se configura entre 1 y 86 400 segundos. Estos son límites de validación, no resultados de una prueba de carga.

Las zonas deben quedar dentro del piso y no solaparse entre sí ni invadir refugio, portal o patio. La generación usa `seed + floorId × 1009 + zoneId.hashCode() × 31`; evita obstáculos, NPC y otras apariciones. Los niveles se reparten cíclicamente dentro del rango. Si la población es menor que la cantidad de niveles del rango, no aparecen todos los niveles a la vez. La validación rechaza una población que no cabe después de 10 000 intentos por zona.

## Partidas anteriores

El esquema pasa a revisión **7** manteniendo `version: 1`. La migración añade catálogo y campos ausentes, asigna nivel 1 a apariciones antiguas y conserva posiciones, personalizaciones, personajes y botín previo. Escribe una copia `.pre-migration-<uuid>.bak` antes de reemplazar el JSON.

**Las cinco zonas se incluyen en la semilla nueva.** Un mundo guardado de una entrega anterior recibe `monsterZones: []` donde falta; no se altera su distribución. Añade las zonas desde root usando la tabla como referencia y ajusta su ubicación a tu propio mapa. No reemplaces `world/` ni `data/` si quieres conservar tu partida.
