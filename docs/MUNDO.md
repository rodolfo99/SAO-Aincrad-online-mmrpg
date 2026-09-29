# Crear y recrear el mundo

`world/world.json` es la fuente de verdad. La pareja **definición + seed** produce los mismos árboles, cristales y colisiones, utilizando `java.util.Random(seed + floorId * 1009)`. El servidor entrega a Angular la lista resultante `props`; el cliente no inventa una segunda distribución. Las viviendas decorativas conservan posiciones fijas en praderas; los edificios explícitos de herrería, sastrería, tienda y casa se colocan desde root y sustituyen viviendas que se solaparían.

Cada piso contiene ID, nombre, subtítulo, bioma (`meadow` o `crystal`), radio 35–100, densidad 0–300, aparición, portal y listas de NPC, monstruos, edificios y nodos de recursos, además de zonas de monstruos por nivel y un patio de entrenamiento opcional. IDs de entidades son únicos en todo el mundo, formados por letras ASCII, números, `_` o `-`, de 1–64 caracteres. Cada portal debe apuntar a un piso existente.

## Añadir un piso

1. Root → **Crear piso** duplica el piso seleccionado, genera otro ID y renombra los IDs de las entidades.
2. Cambia nombre, bioma, aparición y habitantes. Ajusta el portal nuevo para el regreso.
3. En otro piso, establece un portal cuyo destino sea el nuevo ID.
4. Guarda, valida y aplica.

El mundo admite hasta 100 definiciones de piso. Esto no añade automáticamente contenido de cien niveles; el paquete contiene dos. La generación puede colocar menos árboles de los solicitados si no encuentra espacio libre tras el límite de intentos. No hay terreno deformable, mapas importados, editor visual por arrastre ni navmesh.

No coloques aparición, portales o entidades dentro de las viviendas. La validación detecta posiciones fuera del radio y colisiones con props. Los NPC `guide` comparten la misión introductoria por personaje; `smith` abre herrería y mejora general; `tailor` sastrería; `merchant` tienda. Los atributos y crecimiento de enemigos se definen en `monsterSpecies`; Java valida y ejecuta sus reglas. Las misiones siguen en `GameWorld.java`; Ollama no decide estadísticas ni recompensas.

## Catálogo de personajes

`characterOptions` define:

- `genders`: opciones iniciales chico/chica y sus etiquetas.
- `faces`: rostros `calm`, `bold`, `kind`; el renderizador representa esos tres estilos. Añadir un ID por JSON sin ampliar JavaScript usa el rostro base.
- `hairColors` y `skinColors`: paletas iniciales; también se permite seleccionar cualquier color hexadecimal válido.
- `classes`: ID, nombre, descripción, HP base (40–300), daño base (1–60), velocidad (2–9), alcance normal y habilidad configurable.
- `races`: apariencia, retrato, escala, ancho, orejas, barba, cuernos, alas, cola, colores, pasivos y habilidad racial.
- `specializations` y `skills`: restricciones de equipo, ramas y habilidades activas.
- `weaponSets`, `equipmentSets` y `attributeDefinitions`: juegos de armas, ropa y atributos configurables.

El catálogo inicial contiene guerrero, mago y sanador; humano, enano, elfo, elfo oscuro y draconiano. Cait de partidas antiguas se conserva durante la migración. Son opciones de fantasía para este prototipo fan, no una afirmación de que el Aincrad canónico utilice ese sistema de clases/razas. Razas, clases, especialidades, equipo y talentos aportan estadísticas. Las razas también cambian la geometría. La colisión del personaje mantiene el mismo radio para todas las razas.

Root puede editar clases y razas desde tablas del panel; el JSON permite extender paletas y catálogos. Los cambios se activan al aplicar el mundo. Si una definición manual elimina una opción usada por perfiles anteriores, al cargar se recupera la apariencia predeterminada; conserva IDs si quieres mantener sus elecciones.

## Territorios por nivel

La semilla incluye cinco zonas y 26 criaturas: slimes 1–3, perros 4–6, trasgos 7–10, orcos 11–15 y trolls 16–20. `floors[].monsterZones` configura centro, radio, especie, rango, población y reaparición. El catálogo global `monsterSpecies` configura modelos, color, crecimiento, comportamiento y premios; las apariciones manuales también tienen `level`. Consulta [MONSTRUOS-ZONAS.md](MONSTRUOS-ZONAS.md) para límites, edición root y migración de mundos previos.

## Reglas del piso semilla

- Jabalí: 48 HP, 7 daño, 30 EXP y 16 col; respawn de 18 s.
- Centinela: 240 HP, golpe anunciado de 22 daño, 100 EXP y 100 col; respawn de 40 s.
- Participantes cercanos y vivos que hayan golpeado al mismo monstruo o curado a un participante cercano comparten la recompensa.
- Tres jabalíes después de aceptar misión; regreso al guía: 60 col, 40 EXP y 2 pociones, una sola vez por personaje.
- La derrota de un centinela desbloquea el acceso de ese personaje a portales que requieran jefe; todavía no existe un desbloqueo diferente por cada piso.
- Mejora de arma: 50 col, +4 daño, máximo +5.
- Por defecto, cada 100 EXP se gana nivel, +15 HP máximo, +2 daño, 3 puntos de atributo y 1 de talento. Root configura estos valores; consulta PROGRESION.md. Al subir por combate se restaura salud.

## Edificios, recursos y entrenamiento

La nueva definición incluye tres edificios, 22 nodos y un patio con cuatro muñecos. Los catálogos `crafting` y `gathering` especifican oficios, materiales, recetas, precios, recursos, EXP y regeneración. Los nodos están en `floors[].resources`; el patio en `floors[].training`. Cada ID es único junto a NPC, criaturas y edificios.

La generación evita obstáculos, apariciones, portales y el patio. Valida antes de aplicar: edificios y nodos no pueden bloquear el patio. Los muñecos no tienen botín, EXP ni IA hostil. La zona bloquea ataques PvP y de criaturas. Consulta OFICIOS.md y ENTRENAMIENTO.md para los controles y límites.

## Dimensiones de esta entrega

Los radios iniciales son 92 m (Praderas) y 84 m (Bosque), con 255/225 elementos de vegetación. El minimapa y el camino principal siguen el radio del piso activo. La plaza acompaña a la aparición. La ampliación de mundos ya instalados es explícita y conserva sus reglas: consulta [PISOS-AMPLIADOS.md](PISOS-AMPLIADOS.md).
