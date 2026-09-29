# Pisos ampliados

Los dos pisos tienen el doble de radio y cuatro veces la superficie anterior.

| Piso | Radio anterior → actual | Diámetro actual | Superficie aproximada | Vegetación decorativa | Nodos de recursos |
|---|---|---|---|---|---|
| Praderas del comienzo | 46 → 92 m | 184 m | 26 590 m² | 255 árboles | 12 |
| Bosque de cristal | 42 → 84 m | 168 m | 22 167 m² | 225 árboles/cristales | 10 |

El refugio, los talleres y el patio del primer piso conservan su posición. Las zonas de caza se reparten por el terreno nuevo y amplían su radio sin cambiar niveles, población ni recompensas. Se añaden 13 nodos de minería/leñador a los nueve existentes. El camino central llega hasta las proximidades de los bordes; la plaza del segundo piso acompaña a su nueva aparición. Vegetación, hierba, partículas, acantilados, nubes y castillo se adaptan al tamaño.

| Punto | Piso | X | Z |
|---|---|---|---|
| Refugio inicial | 1 | 0 | 16 |
| Centinela | 1 | 0 | −64 |
| Portal norte | 1 | 0 | −76 |
| Aparición del bosque | 2 | 0 | 58 |
| Portal de regreso | 2 | 0 | 63 |
| Centinela de cristal | 2 | 0 | −62 |

El portal del primer piso sigue requiriendo derrotar al Centinela. Se conserva el movimiento autoritativo y la velocidad del personaje; atravesar el mapa requiere más tiempo.

## Instalación nueva

Descomprime el paquete y ejecuta `./scripts/iniciar.sh`, como indica el README. Registra una cuenta de jugador e inicia sesión para acceder. El mundo incluido ya está ampliado; no hay que ejecutar una migración adicional.

## Actualizar una instalación con personajes

Detén el servidor anterior. Extrae esta entrega en una carpeta nueva y conserva la anterior hasta comprobar el arranque. Desde la carpeta nueva `aincrad-seed`, copia únicamente la definición del mundo y los datos de tu instalación anterior:

```bash
AINCRAD_ANTERIOR="/ruta/a/tu/aincrad-seed-anterior"
cp -a "$AINCRAD_ANTERIOR/data" ./data
cp "$AINCRAD_ANTERIOR/world/world.json" ./world/world.json
python3 scripts/ampliar-pisos.py --dry-run
python3 scripts/ampliar-pisos.py
chmod +x scripts/*.sh
./scripts/iniciar.sh
```

Usa una carpeta nueva sin `data/` antes de copiar; así no mezclas dos partidas. Mantén el resto de los archivos `world/` de esta entrega: el actualizador utiliza `world/expansions/floors-large-v1.json`. Python 3 sólo se necesita para esta actualización de un mundo existente; el juego compilado sigue necesitando Java y el navegador.

El script reconoce la distribución compacta del esquema 7, comprueba ambos pisos antes de escribir y cambia sus dimensiones/posiciones. Conserva catálogos, estadísticas, niveles/poblaciones de caza, nombres y configuraciones ajenas a la distribución. Escribe una copia exacta del JSON anterior en `world/backups/`, luego sustituye el archivo de forma atómica. Ejecutarlo otra vez no duplica recursos ni crea otra copia si el mundo ya está ampliado. Puedes pasar otra ruta de mundo como argumento.

Los personajes, la contraseña root y la configuración de Ollama permanecen en `data/`. Al reconectar, el servidor conserva las posiciones transitables y devuelve a la aparición los personajes que hayan quedado dentro de una colisión. No hace falta recrear personajes. Esta entrega exige una cuenta: vincula los perfiles antiguos siguiendo [CUENTAS.md](CUENTAS.md).

Si el script detecta una distribución personalizada, termina sin modificar archivos. En ese caso entra con root en `/admin`, selecciona cada piso y ajusta **Radio (35–100)** y **Árboles (0–300)**; mueve sus portales, apariciones, zonas y recursos según tu diseño. El formulario muestra diámetro y superficie. **Guardar y validar** comprueba el mundo; **Aplicar y reconectar** activa los cambios. Ampliar sólo el radio no redistribuye automáticamente las entidades personalizadas.

## Minimap y gráficos

El minimapa utiliza las dimensiones del piso activo, muestra el diámetro y mantiene dentro del encuadre el portal y las zonas exteriores. El tamaño del terreno y del sendero sigue el mismo cálculo. Las tres calidades gráficas continúan disponibles; la hierba se ajusta a la superficie con un máximo de 24 000 instancias y las partículas tienen un máximo de 600 por piso.

Hay más geometría total; no se ha medido el rendimiento en tu GPU. Si necesitas reducir carga, selecciona **Ajustes gráficos → Ligera**. La ampliación mantiene dos pisos y no añade pisos nuevos.

## Comprobaciones reproducibles

```bash
npm --prefix client ci
npm --prefix client test
npm --prefix client run build
python3 -m unittest discover -s scripts/tests -v
```

Con un servidor **aislado** iniciado sobre copias temporales de mundo/datos, Playwright/Chromium instalado y `AINCRAD_ADMIN_PASSWORD` configurada:

```bash
npm --prefix client run test:expanded-floors
```

Esta prueba comprueba las rutas sobre los obstáculos generados por Java, el panel root, movimiento más allá del borde antiguo, rechazo del borde nuevo, minimapas y ambos portales en sus posiciones finales. Desactiva únicamente el requisito de jefe del primer portal en la copia de prueba; conserva posiciones, criaturas y colisiones. Escribe `docs/expanded-floors-result.json` y capturas `29-` a `32-`.
