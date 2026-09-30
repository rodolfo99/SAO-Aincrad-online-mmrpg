# Arquitectura

Un proceso Spring Boot sirve Angular, `/api` y el WebSocket `/ws` en el mismo origen. La simulación avanza 20 veces por segundo con paso fijo de 50 ms, y publica instantáneas 10 veces por segundo. No se aumenta la distancia de movimiento por recibir más mensajes. El cliente suaviza posiciones y orientaciones con interpolación; no hay todavía predicción y reconciliación completas.

`GameWorld` serializa accesos a jugadores y enemigos. El transporte limita tamaño, número de mensajes y conexiones; `ConcurrentWebSocketSessionDecorator` protege envíos concurrentes y limita el búfer de un receptor lento. El máximo es 32 jugadores, con hasta 40 conexiones incluyendo handshakes. No se ha medido capacidad de producción.

## Protocolo JSON, versión de proyecto 0.2.0

Antes de conectar a `/ws`, el jugador debe registrarse e iniciar sesión mediante `/api/player`. El handshake exige una sesión válida en la cookie `aincrad_player` y un origen permitido; no acepta claves antiguas ni la cookie de root. La sesión se comprueba también durante la partida. Su revocación o caducidad cierra la conexión con código 4401. Véase [CUENTAS.md](CUENTAS.md).

| Cliente → servidor | Datos relevantes |
|---|---|
| `join` | `characterId` propio para continuar; vacío/omitido con `name` y `appearance` para crear. La cuenta procede de la sesión, nunca del mensaje |
| `input` | `seq` creciente, `dx` y `dz` entre −1 y 1; vector normalizado |
| `move` | `x`, `z` del destino; el servidor calcula y sigue la ruta; nunca teletransporta |
| `stop` | Cancela dirección, destino y todos los puntos intermedios; pérdida de foco lo envía |
| `target` | `id` y `kind`: `monster` (incluye muñecos) o `player` del piso |
| `attack` | `skill`: false/true, enfriamiento y alcance en servidor |
| `potion`, `interact`, `portal` | Acciones validadas por distancia y reglas |
| `chat` | `text`, máximo 180 caracteres y una vez por segundo |
| `npcChat` | `npcId`, `text`; distancia, límites y trabajo IA asíncrono |
| `profile` | `name`, `appearance`; se aplica SOLO al emisor y en refugio |
| `progression` | `attributes`, `talents`: mapas de rangos, presupuesto y dependencias en servidor |
| `equip` | `weaponSetId`; selección legal por clase, especialidad y nivel |
| `pvpMode`, `racial`, `redeem` | Activación de ataques PvP, habilidad racial y redención |
| `craft`, `craftUpgrade` | `recipeId` o `itemId`; pago atómico y propiedad |
| `craftEquip`, `craftUnequip` | `itemId`, `slot`; restricciones de equipo |
| `shopBuy`, `shopSell`, `forgeUpgrade` | Material/cantidad, objeto propio o mejora general; proximidad al NPC |
| `shopSellMaterial` | `materialId`, `quantity`; existencias, precio y proximidad validados en Java |
| `shopBuyEquipment` | `productId`; pieza individual, compatibilidad, capacidad y precio del catálogo |
| `gather` | `nodeId`; proximidad, nivel y agotamiento compartido |
| `castSkill`, `bindSkill` | `skillId`, `slot` de 0 a 3; habilidad aprendida |
| `resetTraining` | Borra solo los contadores de práctica propios |
| `ping` | Mantiene sesión; sin mensajes durante 20 s se desconecta |

La dirección continua caduca a los 250 ms si deja de llegar entrada. El destino de clic mantiene una intención de caminar; una tecla de movimiento la cancela. Los campos del cliente que intenten fijar HP, daño, col, XP o posición instantánea no se aplican. Una entrada inválida produce `error`.

`WorldPathfinder` usa A* sobre una malla de 1 m, ocho vecinos y un máximo de 8192 nodos expandidos por búsqueda. Conecta origen/destino exactos a sus vecinos transitables y simplifica puntos solo si el atajo completo está libre. `WorldData.walkableSegment` comprueba ambos extremos con `walkable` y la distancia mínima del segmento a cada prop con su radio y el margen existente de 0,4 m. El borde del piso sigue siendo el disco de radio `floor.radius - 1`. No se incorporan colisiones procedentes de los modelos del navegador.

Java conserva la cola de puntos en memoria y reparte un único presupuesto `player.speed() * dt` entre todos los tramos recorridos en un tick; vuelve a comprobar cada tramo antes de avanzar. WASD mantiene su normalización, caducidad y deslizamiento por ejes. Una entrada neutra o con secuencia antigua no cancela la ruta; una entrada nueva con dirección sí lo hace. Un clic válido reemplaza la cola y limpia la dirección residual. Parada, desconexión, muerte y aparición/cambio de piso vacían toda la cola. Los destinos rechazados conservan la intención anterior. Si se bloquea un tramo después de planificar, se detiene la ruta y se emite un `notice`.

El cliente sigue enviando únicamente `{type:'move', x, z}` y dibujando las posiciones de `state`. La cola no se expone en el protocolo ni se persiste con el perfil. Las rutas no se transfieren entre pisos. Los pasos estrechos que no tengan conexión en la malla pueden rechazarse aunque exista un recorrido continuo. Detalles de activación y pruebas en [MOVIMIENTO-POR-CLIC.md](MOVIMIENTO-POR-CLIC.md).

| Servidor → cliente | Uso |
|---|---|
| `welcome` | ID y perfil propio, sin claves de acceso |
| `state` | Piso, jugadores visibles, enemigos/muñecos, recursos, invocaciones, enfriamientos y tick |
| `skillEffect` | Visual efímero de una habilidad ya aplicada |
| `heal` | Curación ya aplicada, destinatario y cantidad |
| `hit` | Animación y número de daño, ya decidido por el servidor |
| `dialogue` | Texto de misión, ID del NPC y disponibilidad de IA |
| `npcReply` | Respuesta de Spring AI o fallback indicado |
| `chat`, `notice`, `error` | Interfaz y diagnósticos visibles |

El servidor emite algunos eventos efímeros a todas las sesiones y el cliente filtra por piso; las instantáneas contienen únicamente el piso del destinatario. Para un sistema grande se debe trasladar también ese filtro de eventos al transporte.

## Persistencia

`PlayerAuth` guarda las cuentas en `data/accounts.json`, con UUID, usuario normalizado y contraseña derivada mediante PBKDF2-HMAC-SHA256, sal individual y 600 000 iteraciones. Las sesiones aleatorias de 256 bits llegan al navegador en una cookie HttpOnly/SameSite=Strict; el servidor conserva únicamente su hash y caducidad en memoria. Reiniciar el servidor, cambiar/restablecer la contraseña o cerrar la sesión invalida el acceso correspondiente. No se guardan contraseñas en claro ni sesiones nuevas en localStorage.

Cada perfil de personaje tiene un `accountId`. Java verifica su propietario antes de permitir `join`, independientemente de los IDs enviados por el cliente. Una cuenta puede guardar varios personajes y recuperarlos desde otro navegador. El mismo personaje no puede entrar simultáneamente en dos conexiones. Los perfiles anteriores sin propietario se vinculan después de iniciar sesión, acreditando su clave antigua, o mediante recuperación por root; no obtienen acceso anónimo.

`PlayerStore` escribe JSON en `data/players/<uuid>.json` mediante archivo temporal, `force(true)` y rename atómico. Hay guardado periódico cada 5 s, al salir, al editar y al cerrar el proceso. El archivo lock impide dos servidores escribiendo el mismo directorio. Ante JSON corrupto el inicio falla, sin regenerar silenciosamente personajes. La durabilidad ante cortes de energía del propio directorio depende del filesystem; no equivale a una base de datos transaccional de producción.

Los enemigos, su HP y sus temporizadores son estado efímero. Se recrean con la definición al reiniciar o aplicar mundo. Los personajes, progreso y desbloqueo del portal sí persisten.

## Administración y personajes

`AdminAuth`: root, PBKDF2, sesión aleatoria en cookie HttpOnly/SameSite=Strict de una hora, límite de intentos y comprobación de origen/cabecera en mutaciones. No usa la clave de los jugadores. No hay un endpoint público de primer registro de root: el primer arranque requiere contraseña por entorno o asistente de terminal.

`/api/admin/world` valida y respalda cambios; `/reload` guarda, desconecta, recarga y permite reconectar. `/api/admin/characters` nunca entrega claves ni hashes. Root puede editar nombre, apariencia, clase, raza, EXP, col, pociones y reparto de atributos/talentos; mantiene respaldo previo. También puede consultar cuentas, restablecer contraseñas con revocación de sus sesiones y asignar un personaje desconectado a una cuenta existente. La autoedición usa `profile` y no comparte ese endpoint administrativo. Solo funciona en el radio del refugio y lejos de enemigos. No cura ni acepta EXP/col enviados por el jugador.

## Aprendizajes aplicados

- Movimiento normalizado, velocidad decidida por Java y picking relativo al rectángulo real del canvas.
- Entradas de texto no activan ataques ni movimiento; pérdida de foco limpia teclas.
- Angular utiliza señales y detección zoneless, con errores y estados de reconexión visibles.
- Las instantáneas retiran monstruos muertos, y la reaparición vuelve a crear su modelo.
- Gráficos y datos incluidos localmente, sin dependencias de CDN durante la partida.
- Scripts con rutas entrecomilladas, contraseñas sin valor predeterminado y persistencia separada de builds.
- Pruebas que conectan dos clientes reales al Java y verifican la UI del navegador.

Es una implementación nueva basada en las lecciones de proyectos previos; no se reutilizaron ni copiaron recursos de The Mana World o código de Helbreath/Babylon de aquellos paquetes.

## Sistemas añadidos en v0.2.0

`Races`, `Weapons` y `Specializations` validan catálogos y calculan compatibilidad. `Skills` guarda recargas y barra; sus efectos temporales e invocaciones viven en la simulación. Las invocaciones atacan únicamente criaturas y muñecos ofensivos seleccionados, y desaparecen al morir/salir/cambiar de piso o agotarse la duración.

`Crafting` mantiene materiales, EXP por oficio, inventario propio y ranuras. Las transacciones guardan el perfil tras verificar costes; si falla validación o escritura se revierte el estado. `Gathering.Store` registra recibos durables y `lastGatherEvent` evita duplicar material tras reinicio. `PvpJournal` conserva eventos de bajas y reputación con secuencia, para reparar perfiles que quedaron antes del recibo.

`Training` define un patio opcional por piso y contadores propios por sesión. Los muñecos no son monstruos recompensables. No persisten su vida ni los contadores; los enfriamientos del personaje sí se guardan. Solo el dueño recibe sus datos de inventario y entrenamiento en la instantánea.

Endpoints root adicionales: `PATCH /api/admin/characters/{id}/weapons`, `/citizenship` y `/crafting`. Usan la misma autenticación, comprobación de origen/cabecera y respaldo. No hay API pública para otorgar EXP, dinero, objetos o habilidades.

La revisión de esquema del mundo es 7 (`version: 1` conserva el formato general). Al cargar una revisión anterior se realiza migración aditiva y se crea un `.pre-migration-<uuid>.bak` antes de escribir. Las definiciones entregadas de v0.1.0 y de esquema 6 se prueban como fixtures de migración. Los mapas existentes conservan su distribución: coloca edificios, nodos, patio y zonas de monstruos nuevos desde root; la semilla nueva ya los incluye.


## Bestiario y territorios

`Bestiary` valida especies y zonas, calcula crecimiento y genera puntos deterministas después de construir los obstáculos. `Floor.zoneSpawns` es un resultado interno con `@JsonIgnore`; no se acepta como lista de posiciones impuesta por clientes. Los IDs generados comparten la validación de unicidad de las entidades manuales. La simulación instancia ambas listas y añade aparte los muñecos.

Cada monstruo en `state` incluye `model`, `color`, `level`, `zoneId`, `returning`, `defense`, `damage`, `xpReward` y `colReward`. Al morir se retira de la instantánea y reaparece con el mismo ID/nivel. La zona limita la persecución y prevalece sobre el temporizador individual de la especie. Una retirada limpia participación/efectos y bloquea daño hasta llegar al origen; después de ocho segundos se recupera una criatura bloqueada. Los ataques a distancia y curas cooperativas conservan participación.

El cliente usa el mismo constructor de modelos para mundo y vista previa root. El minimapa y panel leen la definición pública; vida, niveles, recompensas y recargas efectivos proceden del servidor. Añadir una especie que reutiliza geometría existente solo requiere configuración. Un modelo nuevo requiere ampliar `Bestiary.MODELS`, el selector Angular y `world-renderer.js`.

## Renderizado del mundo renovado

El cliente separa construcción del entorno (`environment-model.js`), criaturas y articulación (`creature-model.js`), primitivas/lotes (`render-kit.js`) y efectos (`combat-effects.js`). `WorldRenderer` conecta estas fábricas con las instantáneas y eventos existentes. El editor root comparte los modelos del bestiario. Los perfiles gráficos se guardan por navegador y solo cambian densidad decorativa, resolución y sombras; no modifican simulación, permisos, aparición de entidades ni radios de habilidades. Consulte [MUNDO-3D.md](MUNDO-3D.md).

## Correo y catálogo de mercado

`MailService` encapsula el transporte Java hacia Postfix, la cola de trabajo acotada y el envío de prueba root. `PasswordRecovery` construye enlaces desde el origen configurado y solicita su entrega; `PlayerAuth` consume el hash del enlace y cambia la contraseña en una única actualización atómica. El mensaje no autentica por sí mismo. La utilidad Postfix de Compose entrega por IP y mantiene la cola SMTP. Véase [CORREO.md](CORREO.md).

`crafting.shopProducts` define productos reutilizando los conjuntos de equipo. Las compras crean un `Crafting.Item` propio y guardan el coste invertido. Las ventas de recursos usan `Material.sellPrice`, opcional, y la cantidad de `crafting.materials` del emisor autenticado. Ambas operaciones usan el mismo guardado y reversión de los talleres. No se aceptan precio, propietario ni atributos impuestos por el cliente.
