# Oficios, talleres y recolección

Los cuatro oficios pueden aprenderse con un mismo personaje; no excluyen su clase de combate. Cada uno tiene EXP propia: inicialmente 100 EXP por nivel, máximo 50. Crear y mejorar concede experiencia del oficio correspondiente; minería y leñador la obtienen al recolectar. Root configura puntos, recetas, atributos, costes, límites, botín y posiciones.

| Oficio | Lugar | Actividad |
|---|---|---|
| Herrero | Brann (−6, 16), frente a la herrería | Armas, escudos, orbes/tomos y armaduras de cota/placas |
| Sastre | Mira (6, 16), frente a la sastrería | Túnicas, ropa y cuero |
| Minería | Vetas señaladas | Cobre, hierro, mithril y cristal |
| Leñador | Árboles señalados | Pino, roble y madera ancestral |

Tessa (6, 23), junto a la tienda, vende materiales, pociones y equipo básico, y compra recursos de la bolsa y piezas desequipadas. Las tres construcciones tienen modelos 3D, nombres y colisiones. E abre el diálogo del encargado; también puedes usar los botones Oficios y Tienda e Ir al taller. Las compras/recetas exigen estar a un máximo de 4 m del NPC y fuera de combate PvP.

## Fabricar, mejorar y vestir

Se incluyen 84 recetas asociadas a los conjuntos iniciales, para niveles de personaje 1, 5 y 10. Fabricar una receta paga materiales y col de forma indivisible, crea un ID de objeto propio y guarda sus atributos. Mejorar paga el coste base multiplicado por el siguiente rango; incrementa atributos según la receta, limitado por el catálogo. El objeto requiere el nivel y oficio indicados, y tiene tope de mejora configurable.

No hace falta crear el equipo de inicio. Las piezas artesanales sustituyen ropa, principal o secundaria cuando se equipan en el refugio y cumplen clase, género, especialidad y nivel. Dos manos impiden secundaria. Una misma pieza no puede ocupar dos ranuras. Cambiar clase o especialidad desequipa lo incompatible, sin borrarlo. Los objetos no se transfieren entre personajes en esta versión.

La tienda admite 1–50 unidades por compra de materiales o pociones; el equipo se compra por piezas individuales. Los precios proceden del servidor. Vender una pieza devuelve inicialmente el 30 % de los col invertidos en compra/fabricación/mejoras, redondeado hacia abajo; no devuelve materiales. Solo se vende una pieza propia y desequipada una vez. La mochila admite inicialmente 100 piezas, con máximo configurable 200.

## Materiales iniciales

| Material | Precio en tienda (col/unidad) |
|---|---:|
| Hierro | 6 |
| Pino | 3 |
| Fibra | 2 |
| Cuero | 4 |
| Cristal | 12 |
| Cobre | 4 |
| Mithril | 18 |
| Roble | 9 |
| Madera ancestral | 20 |

La fibra y el cuero vienen de botín/tienda; los jabalíes dan 2 fibras y 1 cuero. El guardián da 4 hierros, 2 cristales y 3 pinos. Las nuevas especies también aportan botín: slime (fibra/cristal), perro (cuero), trasgo (cobre/pino), orco (hierro/cuero) y troll (mithril/cristal). Minería y leñador proporcionan recursos mediante nodos compartidos.

## Tipos de recurso

| Recurso | Oficio | Nivel de oficio | Unidades | EXP | Regeneración (s) |
|---|---|---:|---:|---:|---:|
| Veta de cobre | Minería | 1 | 3 | 25 | 20 |
| Veta de hierro | Minería | 1 | 3 | 25 | 25 |
| Veta de mithril | Minería | 2 | 2 | 25 | 35 |
| Cristal arcano | Minería | 2 | 2 | 25 | 35 |
| Pino maderable | Leñador | 1 | 3 | 25 | 20 |
| Roble maderable | Leñador | 2 | 3 | 25 | 30 |
| Árbol ancestral | Leñador | 3 | 2 | 25 | 45 |

Hay 22 nodos iniciales repartidos entre los dos pisos ampliados (12 en Praderas y 10 en el Bosque de cristal). Abre el oficio, pulsa Acercarse y Recolectar, o haz clic en el nodo estando cerca. Se exige estar a 3 m o menos, vivo, fuera de combate PvP y tener el nivel de oficio. Hay recarga de 1,5 s entre recolecciones y agotamiento compartido: otro jugador ve el mismo nodo vacío hasta que regenera. El árbol deja tocón y la veta un marcador; conservan colisión.

El movimiento por clic no calcula rutas: rodea edificios o árboles si es necesario. No todos los árboles decorativos son recolectables: utiliza los señalados con nombre y nivel. No se exige herramienta consumible en esta semilla.

El agotamiento y la entrega de material se registran en `harvest-journal`, con recuperación tras reinicio sin volver a conceder una recolección ya aplicada. Respalda todo `data/` junto al mundo.

## Root

Los formularios **Oficios y recetas**, **Recolección y recursos**, **Edificios** y **Personajes del mundo** permiten editar definiciones, añadir recetas y nodos, colocar estaciones/edificios y ajustar EXP/materiales de un personaje. Guarda y valida antes de aplicar. Recetas y materiales deben tener referencias válidas; los recursos no pueden solaparse con edificios y cada profesión de recolección necesita nodos de su tipo.

El formulario **Especies y crecimiento → Materiales por derrota** edita el botín de cualquier especie, incluidas las creadas por root. Comparte `crafting.monsterDrops` con el editor de oficios. Las cantidades de materiales son fijas por especie; EXP y col se calculan por nivel. Consulta MONSTRUOS-ZONAS.md.


## Bolsa del personaje y mercado

Abre **Bolsa del personaje** desde la barra lateral o **Inventario → Abrir bolsa de recursos**. Los materiales de minería, leñador, botín y compras se guardan en el mismo inventario persistente. Cada tipo se apila hasta 100 000 unidades; el equipo conserva piezas individuales con ID propio.

Acércate al mercader y abre **Tienda → Mercado de suministros**, o vende directamente desde la bolsa estando a su alcance. Cada material muestra unidades, precio y total. Escribe la cantidad o pulsa **Todo**, y después **Vender**. La operación guarda el descuento y las monedas juntos; si falla la escritura se revierte. No admite cantidades fraccionarias, negativas, superiores a tus existencias ni ventas durante combate o lejos del mercader. El máximo de monedas sigue siendo 10 000 000 col.

Root configura **Materiales, precios y botín → Venta / unidad**. Un valor vacío usa la fracción de reventa, redondeada hacia abajo y con mínimo de 1 col cuando la fracción es positiva. Un cero explícito impide vender ese material. La venta no puede superar su precio de compra. Los materiales recolectables de la semilla se pueden vender todos.

## Equipo básico en venta

El catálogo inicial contiene **28 productos de nivel 1**: 10 piezas principales, 8 secundarias y 10 conjuntos de ropa/armadura. Incluye espadas, dagas, mandobles, escudos, báculos, varas y focos, además de prendas para guerrero, mago y sanador, ambos géneros y sus especialidades compatibles.

| Producto inicial | Precio por pieza |
|---|---:|
| Arma principal de una mano | 55 col |
| Arma principal de dos manos | 80 col |
| Secundaria, escudo o foco | 35 col |
| Ropa o armadura | 65 col |

El filtro **Sólo equipo para mi personaje** muestra lo que puedes utilizar. Al quitarlo puedes consultar el resto; no se permite comprar equipo incompatible con tu clase, especialidad, género o nivel. Cada compra entrega una pieza individual en **Equipo en tu bolsa** y descuenta su precio. No consume materiales ni concede EXP de oficio.

Equipa las compras en el refugio con **Principal**, **Secundaria** o **Vestir**. Una pieza con receta asociada puede mejorarse en el taller cumpliendo sus requisitos; los productos básicos comprados no reciben la bonificación inicial de fabricación. Para vender una pieza vuelve al mercader y desequípala antes en el refugio. La reventa aplica la fracción configurada al col invertido en compra y mejoras.

En root, **Equipo en venta → Añadir producto** permite elegir arma/escudo/foco o ropa/armadura, conjunto y parte, nombre, precio y disponibilidad. Los requisitos y atributos se heredan del conjunto. Guarda y aplica el mundo. Una lista vacía retira la venta de equipo y conserva la de materiales. Los mundos anteriores sin `shopProducts` reciben el catálogo básico al cargarse; se incluye en el editor y se guarda al guardar el mundo, sin modificar la bolsa de los jugadores.
