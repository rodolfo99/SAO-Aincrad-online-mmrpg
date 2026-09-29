# Especialidades y habilidades activas

Cada personaje elige una especialidad de su clase al crearse. Puede cambiarla en el refugio si está permitida la redistribución. Root puede editar la selección y todos los datos. Cambiar especialidad devuelve el árbol aprendido y desequipa piezas incompatibles; conserva objetos, EXP, monedas y vida actual (limitada a la nueva máxima).

| Especialidad | Armas permitidas | Armadura |
|---|---|---|
| Espadachín | Espada, daga, mandoble | Cuero y cota |
| Asesino | Dagas en ambas manos | Cuero |
| Tanque | Espada y escudo | Placas |
| Mago: fuego, hielo, agua o tierra | Báculos, varas, orbes y tomos | Túnica |
| Sanador | Varas, báculos, escudos, orbes y tomos | Túnica y cuero |

El asesino recibe −10 PV, +5 % crítico y +0,3 velocidad; tanque +20 PV, +2 defensa y −0,3 velocidad. Root configura estos pasivos, restricciones y efectos. La especialidad asesino no modifica ciudadanía.

## Ramas

Además de los 15 talentos pasivos compartidos por clase, hay 33 nodos de desbloqueo de habilidades, con coste 1 y un rango. Cada rama enlaza sus nodos en niveles 2 → 4 → 6. Los magos comparten la rama de invocación; sanador puede combinar sus tres ramas según puntos disponibles.

| Clase / especialidad | Rama | Habilidades en orden |
|---|---|---|
| Guerrero / Espadachín | Espadachín | Barrido de acero → Corte cruzado → Ritmo del duelista |
| Guerrero / Asesino | Asesino | Golpe furtivo → Veneno de sombra → Paso de sombra |
| Guerrero / Tanque | Tanque | Golpe de escudo → Desafío del bastión → Muralla protectora |
| Sanador / compartida | Maldiciones | Debilidad → Marchitar → Sello de aflicción |
| Sanador / compartida | Bendiciones | Bendición vital → Gracia veloz → Santuario |
| Sanador / compartida | Curación | Restauración → Coro de vida → Renacer del alba |
| Mago / Fuego | Fuego | Proyectil de fuego → Tormenta ígnea → Ascua compartida |
| Mago / Hielo | Hielo | Lanza de hielo → Prisión de escarcha → Armadura glacial |
| Mago / Agua | Agua | Oleada → Marea reparadora → Fluir compartido |
| Mago / Tierra | Tierra | Lanza de roca → Terremoto → Piel de piedra |
| Mago / compartida | Invocación | Invocar familiar → Invocar guardián → Invocar ancestral |

## Uso y efectos reales

Aprende en **Atributos y talentos**; guarda en el refugio. En **Habilidades activas** asigna teclas 1–4. Q y T siguen siendo acciones independientes. La barra y las recargas persisten.

Daño o maldición requieren objetivo seleccionado y alcance válido. Un ataque de área contra una criatura afecta criaturas cercanas al objetivo; no activa PvP por alcanzar accidentalmente a un jugador. Para dañar jugadores se exige seleccionar uno y activar PvP. Los círculos muestran el área visualmente.

La cura/mejora individual se aplica al aliado seleccionado al alcance, o a uno mismo. Las áreas de apoyo se centran en quien las lanza. El aliado del patio permite probar ambas variantes. Maldiciones y mejoras se acumulan entre habilidades/casters distintos; repetir la misma habilidad del mismo personaje sustituye su efecto. El límite es 32 efectos por objetivo y se respetan los topes de estadísticas.

Provocación obliga a las criaturas cercanas a perseguir al tanque durante la duración; no controla jugadores. Las invocaciones siguen al dueño y atacan cada 1,5 s su criatura/muñeco ofensivo seleccionado dentro del alcance. Solo hay una invocación por dueño; desaparece por expiración, muerte, salida o cambio de piso. Las invocaciones no atacan jugadores.

Los efectos temporales e invocaciones se borran al reiniciar o aplicar mundo. Las recargas se guardan para no poder reiniciarlas reconectando. No hay maná, costes de reactivos ni IA autónoma avanzada de mascotas en esta semilla.

## Parámetros iniciales

Para daño, potencia es multiplicador del daño del personaje (más `skillPower`). En maldiciones indica daño periódico por segundo; en curación, cantidad base a la que se añade curación y parte del daño. En invocación, daño base del acompañante. Consulta el tipo antes de comparar cifras. Las duraciones y áreas se expresan en segundos y metros de la simulación.

| Habilidad | Tipo | Potencia | Alcance | Radio | Duración | Recarga |
|---|---|---:|---:|---:|---:|---:|
| Barrido de acero | damage | 1.3 | 3 | 3 | 0 | 8 |
| Corte cruzado | damage | 2 | 3 | 0 | 0 | 12 |
| Ritmo del duelista | buff | 0 | 0 | 0 | 8 | 25 |
| Golpe furtivo | damage | 2.2 | 3 | 0 | 0 | 10 |
| Veneno de sombra | curse | 5 | 3 | 0 | 6 | 18 |
| Paso de sombra | buff | 0 | 0 | 0 | 6 | 22 |
| Golpe de escudo | damage | 1.2 | 3 | 0 | 0 | 10 |
| Desafío del bastión | taunt | 0 | 6 | 6 | 5 | 18 |
| Muralla protectora | buff | 0 | 0 | 4 | 8 | 28 |
| Debilidad | curse | 0 | 9 | 0 | 8 | 14 |
| Marchitar | curse | 6 | 9 | 0 | 6 | 18 |
| Sello de aflicción | curse | 4 | 9 | 3 | 6 | 24 |
| Bendición vital | buff | 0 | 9 | 0 | 10 | 20 |
| Gracia veloz | buff | 0 | 9 | 4 | 8 | 24 |
| Santuario | buff | 0 | 0 | 6 | 6 | 28 |
| Restauración | heal | 50 | 9 | 0 | 0 | 10 |
| Coro de vida | heal | 45 | 0 | 6 | 0 | 16 |
| Renacer del alba | heal | 70 | 0 | 6 | 0 | 24 |
| Proyectil de fuego | damage | 1.7 | 12 | 0 | 0 | 8 |
| Tormenta ígnea | damage | 1.4 | 11 | 4 | 0 | 16 |
| Ascua compartida | buff | 0 | 0 | 4 | 8 | 24 |
| Lanza de hielo | damage | 1.7 | 12 | 0 | 0 | 8 |
| Prisión de escarcha | curse | 0 | 11 | 4 | 6 | 18 |
| Armadura glacial | buff | 0 | 9 | 0 | 8 | 24 |
| Oleada | damage | 1.5 | 11 | 3 | 0 | 10 |
| Marea reparadora | heal | 30 | 0 | 5 | 0 | 18 |
| Fluir compartido | buff | 0 | 0 | 5 | 8 | 24 |
| Lanza de roca | damage | 2.1 | 9 | 0 | 0 | 11 |
| Terremoto | damage | 1.5 | 9 | 5 | 0 | 20 |
| Piel de piedra | buff | 0 | 9 | 0 | 8 | 25 |
| Invocar familiar | summon | 12 | 10 | 0 | 20 | 35 |
| Invocar guardián | summon | 22 | 12 | 0 | 25 | 45 |
| Invocar ancestral | summon | 32 | 12 | 0 | 30 | 55 |

Root edita especialidades, estilos/armas permitidos, ramas, requisitos, efectos, potencia, objetivos implícitos por tipo, alcance, radio, duración y recarga. Una habilidad necesita un talento válido de su clase y compatible con sus especialidades. Los tipos ejecutables son los de esta tabla; nuevas mecánicas requieren Java y visuales.
