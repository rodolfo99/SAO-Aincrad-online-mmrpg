# Mundo 3D · revisión visual integral

Esta revisión lleva el entorno, el bestiario y los efectos al estilo de los personajes detallados. El resultado es **fantasía 3D estilizada**, con geometría suave y materiales propios. No es un mundo fotorrealista ni una conversión automática de las ilustraciones en modelos 3D.

## Qué ha cambiado

- **Ambos pisos:** terreno con variación de color, bordes rocosos de la isla, adoquines, plaza, vegetación, partículas ambientales y castillo lejano con torres y almenas. La pradera y el bosque de cristal conservan paletas distintas.
- **Árboles:** troncos curvos, raíces, ramas y grupos de hojas. Pino, roble y árbol ancestral tienen diferencias de silueta o color. La calidad gráfica cambia la densidad de hojas y hierba.
- **Edificios:** tejados con tejas superpuestas, vigas, base de piedra, puertas, ventanas, chimeneas, faroles y toldos. La herrería incluye fragua y yunque; la sastrería, rollos de tela y maniquí; la tienda, cajas, barriles y mercancía.
- **Recursos:** vetas de mineral con afloramientos del color configurado y árboles recolectables. Al agotarse, desaparece la parte recolectable y permanece la base; reaparece cuando lo ordena el servidor.
- **Bestiario:** jabalí, perro, slime, trasgo, orco, troll y Centinela tienen nuevos cuerpos, rostros y materiales. Los cuadrúpedos y humanoides articulan sus extremidades; los slimes se deforman al desplazarse. Los muñecos distinguen paja, armadura y práctica de curación.
- **Magia y combate:** golpes con destellos, trazas de ataques a distancia, curación, invocaciones y efectos distintos de fuego, hielo, agua, tierra, luz y sombra. El círculo de una habilidad conserva el radio enviado por el servidor. Los efectos son visuales: no calculan daño ni amplían su alcance.
- **Portal e interfaz:** arco de piedra, detalles metálicos, cristal animado y superficie luminosa; paneles y rótulos con mejor contraste. Los círculos de enemigos aparecen al seleccionarlos o durante un aviso de ataque.

Las nueve ilustraciones originales permanecen en el proyecto. Esta revisión crea las formas y superficies mediante Three.js; no añade servicios de descarga, modelos externos ni nuevas dependencias.

## Usarlo

Arranca como antes con `./scripts/iniciar.sh` y abre `http://localhost:8080`. Dentro del juego pulsa **⚙ Ajustes gráficos**. El cambio se aplica sin cerrar la partida y se recuerda en ese navegador.

| Calidad | Vegetación | Sombras dinámicas | Límite de densidad de píxeles |
|---|---|---|---|
| Ligera | Reducida | Desactivadas | 1× |
| Equilibrada, inicial | Media | Activadas, mapa 1536 | 1,25× |
| Alta | Densa | Activadas, mapa 2048 | 1,75× |

El límite se aplica solo si la pantalla tiene esa densidad; no aumenta una pantalla de 1×. Personajes, recursos, criaturas, objetivos y áreas de habilidades siguen presentes en las tres opciones. La vista de personajes y bestiario también limita la densidad al crear su canvas. El mundo dibuja hasta 30 fotogramas por segundo y las vistas previas se pausan fuera de pantalla o al perder el foco. **30 es un límite de dibujo, no una garantía de rendimiento.**

En `/admin`, **Especies y crecimiento** permite seleccionar el modelo y color de cada criatura. La vista usa la misma fábrica de mallas que la partida. Guarda y aplica para que los cambios lleguen al mundo. Los formularios existentes siguen controlando posición y tipo de talleres, recursos, árboles, zonas, niveles y equipo.

## Compatibilidad

El esquema del mundo sigue siendo **7**. No es necesario borrar `world/`, `data/`, la contraseña root ni los personajes. Al actualizar, conserva tus archivos de configuración y datos; sustituye el código/cliente compilado por los del paquete nuevo. Las ubicaciones y colisiones continúan saliendo del servidor. Los adornos de suelo no añaden obstáculos ni desniveles transitables ficticios.

El servidor Java, Maven, Docker Compose, Node, Ollama y Spring AI mantienen la configuración existente. Este cambio se concentra en el cliente; no cambia estadísticas, permisos, IA ni las reglas de PvP y profesiones.

## Código

| Archivo en `client/src/app/` | Responsabilidad |
|---|---|
| `render-kit.js` | Primitivas, superficies, agrupación por material y lotes espaciales |
| `environment-model.js` | Terreno, árboles, edificios, recursos, portal, ambiente y calidades |
| `creature-model.js` | Bestiario, muñecos, invocaciones y articulaciones |
| `combat-effects.js` | Efectos elementales, golpes, trazas y evolución temporal |
| `world-renderer.js` | Integración, luces, sombras, selección, eventos y liberación de recursos |
| `avatar-model.js` | Jugadores, habitantes, ropa, armaduras y armas detallados |

Los grupos de hojas utilizan instancias en la GPU. La decoración estática se agrupa por material y zona espacial para conservar el descarte fuera de cámara. Las articulaciones y las raíces seleccionables de entidades y recursos se mantienen separadas. Se liberan geometrías, materiales, texturas y mapas de sombra al cambiar de piso o calidad. Hay un límite de 48 efectos transitorios simultáneos; al alcanzarlo se retira el más antiguo.

## Verificación

```bash
cd client
npm ci
npm test
npm run build
```

Con un servidor **aislado** arrancado:

```bash
# Lee /api/world y verifica las fábricas gráficas reales en una escena de inspección.
AINCRAD_URL=http://localhost:8080 npm run test:world-visual

# Crea un perfil y modifica el mundo de prueba desde root.
AINCRAD_URL=http://localhost:8080 AINCRAD_ADMIN_PASSWORD='tu-clave-de-pruebas' npm run test:world-browser
```

Requieren Chromium de Playwright o `CHROMIUM_PATH`. No ejecutes la segunda prueba contra una partida de producción: modifica el color del Centinela y acerca temporalmente el portal al refugio en el mundo aislado para comprobar el cambio de piso.

Los informes están en `world-visual-result.json` y `world-browser-result.json`. Las capturas `20-` a `23-`, `creature-*` y `detail-*` son inspecciones de los modelos reales, con una escena preparada para verlos; `24-` en adelante proceden del cliente Angular con el servidor activo. No son imágenes promocionales generadas de un resultado futuro.

La comprobación gráfica usa Chromium con WebGL por software (SwiftShader), una página activa cada vez. No es una medición de FPS en una GPU física, una prueba de muchos jugadores simultáneos ni una validación de todos los mundos que pueda configurar root. Para conseguir acabado fotorrealista todavía harían falta modelos artísticos, texturas PBR y animaciones elaboradas específicamente.

## Adaptación a los pisos ampliados

`floor-layout.mjs` comparte dimensiones entre el escenario, el minimapa y el resumen de root. El sendero usa el radio actual, la plaza acompaña a la aparición y el castillo/nubes quedan fuera de la isla. Se escalan hierba, partículas y acantilados con límites de instancias. Las pruebas y capturas de la revisión visual anterior corresponden al mapa compacto; la ampliación tiene su registro propio en `expanded-floors-result.json`.
