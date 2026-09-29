# Personajes 3D · revisión visual

El modelo de jugadores y NPC se ha sustituido por una interpretación más detallada de las ilustraciones: proporciones adultas, superficies suaves, ojos con iris y párpados, nariz y labios modelados, orejas, mechones, manos y ropa por capas. Los materiales distinguen piel, cabello, tejido, cuero, metal y cristales. La iluminación de entorno produce reflejos en el equipo; el tejido y la piel tienen un relieve sutil calculado por el shader.

**El resultado sigue siendo estilizado.** Es una mejora sobre los personajes geométricos anteriores, no un modelo humano fotorrealista ni una conversión automática de los retratos a 3D. No incluye escaneo facial, texturas fotográficas de piel, pelo por fibras, captura de movimiento o ropa simulada. Las nueve ilustraciones originales se conservan.

## Cómo verlo

1. Arranca el proyecto con `./scripts/iniciar.sh` y abre `http://localhost:8080`.
2. En la creación del personaje, usa **Cuerpo** o **Rostro**. Arrastra la vista para girar. Con el canvas enfocado también sirven las flechas izquierda y derecha.
3. Cambia género, rostro, raza, pelo, piel, clase y especialidad. La vista utiliza el equipo disponible para ese personaje y nivel.
4. Dentro del juego, abre **Mi personaje** para cambiar el aspecto en el refugio. Pulsa **Guardar personaje** para aplicarlo.
5. En `/admin`, selecciona un personaje para editarlo como root. Las vistas de **Atuendos y armaduras** y **Armas** utilizan el conjunto concreto seleccionado, incluso antes de guardarlo.

Lyra tiene cabello castaño corto, capa verde, armadura clara y libro, acordes con su retrato. El herrero, la sastre y la mercader tienen apariencias propias. Estas apariencias de NPC se definen por función en el cliente; el formulario de habitantes sigue configurando nombre, función y ubicación.

Las cinco razas conservan altura/anchura, paletas, ojos, orejas, barba, cuernos, cola y alas del catálogo root. Las armas principales/secundarias siguen la selección del servidor. Rodillas y codos se articulan al caminar; la mano auxiliar sigue el agarre del arma cuando esta requiere ambas manos. Las capas y colas tienen una oscilación visual, sin física de tela.

## Implementación

| Archivo | Responsabilidad |
|---|---|
| `client/src/app/avatar-model.js` | Anatomía, pelo, prendas, armas, materiales, articulación, identidades de NPC y entorno de iluminación |
| `client/src/app/world-renderer.js` | Integración en el mundo y vista previa, giro, encuadre, iluminación y liberación de recursos |
| `client/src/app/character-editor.component.ts` | Controles de cuerpo/rostro y personalización |

No hay descargas de modelos durante el juego ni nuevas dependencias. Las formas se generan con Three.js y los materiales se agrupan dentro de cada articulación para reducir llamadas de dibujo. La vista previa pausa fuera de pantalla y al perder el foco. El mundo no renderiza sin una partida ni con la pestaña desenfocada; la conexión y simulación del servidor siguen activas. Los detalles faciales pequeños se ocultan a más de 18 unidades de la cámara en el mundo, conservando siempre la anatomía principal.

El esquema del mundo continúa en **7**. No cambian los perfiles guardados, contraseñas, reglas de combate, estadísticas, colisiones ni protocolos del servidor. No hace falta borrar `data/` o `world/` para actualizar.

## Verificación reproducible

```bash
cd client
npm ci
npm test
npm run build
npm run test:avatar-visual
```

La última orden requiere Chromium de Playwright o `CHROMIUM_PATH` y abre un servidor temporal de archivos en loopback. Verifica diez combinaciones de raza/género, clases y niveles, errores WebGL, giro, encuadre y estabilidad de recursos tras doce cambios. Guarda capturas reales del renderizador y métricas en `docs/avatar-visual-result.json`.

`npm run test:avatar-browser` requiere además un servidor de juego **aislado**, `AINCRAD_URL` y `AINCRAD_ADMIN_PASSWORD`; crea y modifica un personaje de prueba. Comprueba Angular, movimiento, Lyra, autoedición, edición root del perfil guardado y recuperación de identidad. No debe ejecutarse contra una partida de producción.

Las capturas `docs/screenshots/avatar-*.png` representan el render real, no imágenes de un resultado futuro. El detalle adicional no se ha medido en la GPU del usuario ni con una concurrencia masiva. Para llegar al acabado de los retratos aún harían falta modelos artísticos con topología y texturas PBR elaboradas específicamente, y una posterior optimización para el navegador.
