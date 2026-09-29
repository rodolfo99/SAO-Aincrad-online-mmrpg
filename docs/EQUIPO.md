# Conjuntos, ropa y armaduras

Se incluyen **30 conjuntos** de ropa/armadura y **30 juegos de armas**, con niveles 1, 5 y 10. Espadachín, asesino y tanque tienen familias propias; mago y sanador conservan sus vestimentas. Todas las familias de ropa incluyen chico/chica. Túnicas, cuero, cota y placas se representan mediante geometría procedural, con torso, cinturón, hombreras, brazales, botas/grebas, capa y casco según configuración. Las variantes chico/chica modifican la silueta y pueden tener colores, atributos y piezas diferentes.

Cada personaje equipa automáticamente el conjunto de mayor `minLevel` que cumpla clase, especialidad, estilo permitido y género, salvo que vista una pieza artesanal compatible. Cambiar clase/género o ganar niveles vuelve a resolver el conjunto en el servidor. No se concede una armadura de nivel superior porque el cliente la dibuje o la solicite. Los oficios crean objetos persistentes y mejorables. Hay ranuras de ropa, principal y secundaria; no hay intercambio entre jugadores ni selección independiente de cada hombrera/bota o texturas GLTF.

## Editor root

En **Atuendos y armaduras**:

1. Selecciona un conjunto existente o pulsa **Añadir nuevo conjunto**. La opción nueva crea una definición con atributos vacíos y un nivel posterior a los existentes de esa clase/género. **Duplicar conjunto** conserva los atributos del seleccionado y asigna un ID y nivel nuevos.
2. Define nombre, clase, género, nivel requerido, estilo, tela, metal, detalle, capa y casco.
3. Añade atributos desde el selector y cambia sus valores.
4. Guarda y valida el mundo; aplica para reconectar a los jugadores y actualizar las estadísticas/modelos.

**Eliminar conjunto** retira la definición del formulario. Se conserva un conjunto compatible de nivel 1 por clase, especialidad y género para que todos los personajes puedan representarse. Las combinaciones clase/género/nivel/especialidad son únicas, y no puede haber dos conjuntos iniciales equivalentes para una misma especialidad; cambia el nivel si duplicaste manualmente un registro en JSON. El ID sirve para identificarlo, no para autorizarlo.

## Atributos configurables

El catálogo `characterOptions.attributeDefinitions` define `id`, nombre, efecto, mínimo, máximo y paso del formulario. Puedes **Crear atributo personalizado**, asignarle un efecto o dejarlo descriptivo, y luego añadirlo a cualquier conjunto. Los valores del equipo se guardan en `equipmentSets[].attributes`.

| Efecto | Cómo se aplica |
|---|---|
| `maxHp` | Se suma a vida base de clase y bono de nivel |
| `damage` | Se suma a daño de clase, mejora de espada y bono de nivel |
| `defense` | Reduce cada golpe enemigo; siempre entra al menos 1 daño |
| `speed` | Se suma a velocidad base de la clase |
| `criticalChance` | Porcentaje de crítico; daño ×1.5 si ocurre |
| `healing` | Se suma a la curación del sanador |
| `skillPower` | Se suma al multiplicador de habilidad ofensiva |
| `cooldownReduction` | Reduce el enfriamiento Q y de habilidades activas, con mínimo de 1 s |
| `none` | Atributo descriptivo visible en inventario, sin efecto sobre estadísticas |

Puedes crear «Fuerza» con efecto `damage` y sumará daño; o «Rareza» con `none` para mostrar información. Varios atributos con el mismo efecto se suman. El servidor verifica nombres, valores finitos y rangos; el paso del formulario guía la edición, y los efectos de vida/daño/defensa se redondean al resolver estadísticas.

Límites de la simulación: vida máxima 30–500, daño base 1–150, defensa 0–40, velocidad 2–10 unidades/s y crítico 0–50 %. La habilidad de clase configura su multiplicador (×2 guerrero, ×2.2 mago); un crítico puede multiplicarlo por 1.5. Estos límites evitan que una configuración accidental invalide el movimiento o el combate; se encuentran explícitos en `GameWorld.Player`.

Los conjuntos de nivel 1 conservan los valores iniciales de vida/ataque, y añaden defensa según clase. Niveles 5 y 10 añaden vida, daño, defensa y crítico. Las ilustraciones generadas acompañan al proyecto, pero estas armaduras son mallas de código originales, no prendas raster intercambiadas sobre una imagen.

## Armas principal y secundaria

El catálogo `weaponSets` distingue espada, daga, mandoble, báculo, vara, escudo, orbe y tomo. Escudos, orbes y tomos solo van en secundaria. Un mandoble o báculo de dos manos ocupa ambas ranuras. Una pieza de una mano puede ocupar secundaria cuando su especialidad lo permite.

En Inventario el personaje puede elegir un juego desbloqueado dentro del refugio. La opción automática resuelve el mayor nivel permitido. Root edita juegos, atributos, colores y especialidades y selecciona el equipo de cada personaje. Equipar no cura ni concede niveles. Cambiar un juego de armas retira las sustituciones artesanales de ambas manos; los objetos continúan en la mochila.

La herrería fabrica armas y escudos; sastrería fabrica túnicas y cuero, y herrería cota/placas. Las piezas artesanales guardan su propia copia de atributos y apariencia. Una modificación posterior de la plantilla no reescribe objetos existentes. Cambiar clase/especialidad desequipa lo incompatible, conservándolo en el inventario.
