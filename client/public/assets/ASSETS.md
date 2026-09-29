# Recursos visuales y procedencia

Ilustraciones generadas para este proyecto con la herramienta integrada de generación de imágenes el 28 de septiembre de 2026. No se descargaron recursos oficiales de SAO ni de otros juegos. Los archivos se incorporan íntegros, sin recortar ni alterar la transparencia del árbol.

| Archivo | Uso |
|---|---|
| aincrad-landscape.png | Fondo del acceso y referencia en administración |
| lyra.png | Diálogo y diario de la guía original Lyra |
| boar.png | Bestiario ilustrado |
| oak.png | Árbol con alfa, mostrado en el diario y referencia de entorno |

El render en tiempo real usa geometría Three.js original en `world-renderer.js` y `avatar-model.js`. No se afirma que las imágenes sean modelos 3D riggeados. Los personajes tienen superficies suaves, anatomía y materiales más detallados, pero su acabado continúa siendo estilizado y más sencillo que las ilustraciones.

Prompts utilizados (herramienta integrada, no CLI):

1. “Use case: stylized-concept. Asset type: panoramic background illustration for the actual start screen of an Angular 3D MMORPG fan prototype called Aincrad: Ecos del primer piso, inspired by Sword Art Online. Create a beautiful high quality anime fantasy landscape, wide 16:9 composition: enormous tiered floating castle hanging high in pale azure sky, green plateaus and a medieval starting town with warm terracotta roofs in foreground, winding ivory stone path through sunlit flower meadows, large leafy ancient trees, far waterfalls falling off floating islands, tiny birds, luminous clouds, painterly cel shaded anime environment concept art, crisp detail and airy atmosphere. Keep the left quarter and bottom third relatively quiet for HTML interface text overlaid separately. No text, no letters, no logos, no watermark, no screenshot or user interface. This is an original illustration for a working game asset.”
2. “Use case: stylized-concept. Asset type: square character portrait for a real fantasy MMORPG journal and NPC dialogue. An original adult female swordswoman guide named Lyra, short chestnut hair, emerald eyes, teal travel cape over ivory and bronze light armor, subtle friendly confident smile, half-length three-quarter view holding a leather map journal, soft forest bokeh backdrop. Refined high-quality anime illustration inspired by the visual world of Sword Art Online, crisp clean linework, rich painterly soft daylight, emotionally engaging. Original character, no text, no logo, no user interface. Square composition.”
3. “Use case: stylized-concept. Asset type: square bestiary illustration for an actual fantasy anime MMORPG. A single fierce but appealing wild boar with reddish chestnut fur, prominent curved ivory tusks, crystalline teal markings along its shoulders, sturdy powerful shape, standing in a luminous green forest glade in three-quarter profile. Detailed high quality anime game concept art, crisp clean silhouette, warm sunlight, moss, tiny drifting pollen. Full body, comfortably within all edges, no text, no logos, no UI.”
4. “Use case: stylized-concept. Asset type: isolated transparent game environment sprite for a 3D anime MMORPG. A single magnificent broadleaf oak tree, lush layered moss-green and spring-green canopy, thick twisting brown trunk with visible roots, beautiful hand-painted anime game style, three-quarter view from slightly above, whole tree including all foliage and roots comfortably within image. No background, no ground plane, no sky, no cast shadow outside tree, genuine transparent alpha. No labels, no grid, no text, no logo. Strong clear silhouette, tasteful cel shading and sunlit highlights.”

## Retratos de razas · v0.2.0

Generación original con image_gen integrado; sin imágenes de referencia. Ilustraciones de selección, no modelos 3D. Los modelos del mundo se construyen por código.

### client/public/assets/race-human.png

Prompt final:

```text
Use case: stylized-concept. Asset type: square race selection portrait for an anime fantasy MMORPG client. Primary request: Humans: two adult adventurers, a brown-haired man with a short steel sword and a dark-haired woman with a small healer wand, warm natural human skin tones. Practical teal and cream traveling outfits. Composition: show both adults side by side from knee upwards, complete faces, horns or ears comfortably inside frame, confident relaxed poses, readable at thumbnail size. Style: polished original hand-painted anime game concept art, soft cel shading, clean silhouettes, fine material details. Backdrop: softly blurred floating island woodland and stone ruins, restrained atmospheric teal background. Lighting: gentle golden afternoon rim light. No text, labels, logos, watermark or UI. Both characters fully clothed in practical adventuring attire. Match the coherent collectible portrait-card visual language of a fantasy RPG.
```

### client/public/assets/race-dwarf.png

Prompt final:

```text
Use case: stylized-concept. Asset type: square race selection portrait for an anime fantasy MMORPG client. Primary request: Dwarves: two adult short, broad, sturdy adventurers, a copper-bearded man with an iron hammer and a red-haired woman with a round shield. Practical leather and bronze traveling armor, mountain forge accents. Composition: show both adults side by side from knee upwards, complete faces, horns or ears comfortably inside frame, confident relaxed poses, readable at thumbnail size. Style: polished original hand-painted anime game concept art, soft cel shading, clean silhouettes, fine material details. Backdrop: softly blurred floating island woodland and stone ruins, restrained atmospheric teal background. Lighting: gentle golden afternoon rim light. No text, labels, logos, watermark or UI. Both characters fully clothed in practical adventuring attire. Match the coherent collectible portrait-card visual language of a fantasy RPG.
```

### client/public/assets/race-elf.png

Prompt final:

```text
Use case: stylized-concept. Asset type: square race selection portrait for an anime fantasy MMORPG client. Primary request: Elves: two adult slender adventurers with long pointed ears, a golden-haired man with a tall wooden mage staff and a pale-haired woman with a wand. Moss green and ivory robes and light armor, woodland details. Composition: show both adults side by side from knee upwards, complete faces, horns or ears comfortably inside frame, confident relaxed poses, readable at thumbnail size. Style: polished original hand-painted anime game concept art, soft cel shading, clean silhouettes, fine material details. Backdrop: softly blurred floating island woodland and stone ruins, restrained atmospheric teal background. Lighting: gentle golden afternoon rim light. No text, labels, logos, watermark or UI. Both characters fully clothed in practical adventuring attire. Match the coherent collectible portrait-card visual language of a fantasy RPG.
```

### client/public/assets/race-darkelf.png

Prompt final:

```text
Use case: stylized-concept. Asset type: square race selection portrait for an anime fantasy MMORPG client. Primary request: Dark elves: two adult slender adventurers with long pointed ears, naturally muted violet-gray skin, silver-white hair and bright lavender eyes. A man with a sword and a woman with a crystal wand. Respectful, confident friendly expressions, elegant dark purple and silver traveling clothes. Composition: show both adults side by side from knee upwards, complete faces, horns or ears comfortably inside frame, confident relaxed poses, readable at thumbnail size. Style: polished original hand-painted anime game concept art, soft cel shading, clean silhouettes, fine material details. Backdrop: softly blurred floating island woodland and stone ruins, restrained atmospheric teal background. Lighting: gentle golden afternoon rim light. No text, labels, logos, watermark or UI. Both characters fully clothed in practical adventuring attire. Match the coherent collectible portrait-card visual language of a fantasy RPG.
```

### client/public/assets/race-draconian.png

Prompt final:

```text
Use case: stylized-concept. Asset type: square race selection portrait for an anime fantasy MMORPG client. Primary request: Draconians: two adult humanoid dragon adventurers, a man with terracotta scales and a woman with teal scales, curved short horns, visible tails and small folded wings. Expressive human-like faces with draconic details, amber eyes, modest cream and bronze adventure armor. One carries a shield, the other a healer wand. Composition: show both adults side by side from knee upwards, complete faces, horns or ears comfortably inside frame, confident relaxed poses, readable at thumbnail size. Style: polished original hand-painted anime game concept art, soft cel shading, clean silhouettes, fine material details. Backdrop: softly blurred floating island woodland and stone ruins, restrained atmospheric teal background. Lighting: gentle golden afternoon rim light. No text, labels, logos, watermark or UI. Both characters fully clothed in practical adventuring attire. Match the coherent collectible portrait-card visual language of a fantasy RPG.
```



## Bestiario de zonas · esquema 7

Slime, perro, trasgo, orco y troll se construyen con geometría original en `client/src/app/world-renderer.js`, con material/color configurables. La misma geometría aparece en el mundo y en la vista previa root. Las capturas `docs/screenshots/monster-*.png` documentan ese render real de Three.js; no son ilustraciones generadas ni texturas necesarias para ejecutar el juego. Esta ampliación no añade archivos raster de juego.

## Revisión de personajes · 29 de septiembre de 2026 (UTC)

`avatar-model.js` define mallas articuladas originales de jugadores y NPC, con materiales físicos, detalle de tejido/piel calculado en shaders, armaduras curvas y accesorios. El entorno de reflexión se genera localmente con `RoomEnvironment` de Three.js, incluido en la dependencia existente. No se han descargado modelos ni texturas externos ni se han creado nuevos retratos. `docs/screenshots/avatar-*.png` son capturas reales de esos modelos, utilizadas para revisión y documentación.

## Mundo 3D procedural · 29 de septiembre de 2026

La revisión integral no añade imágenes raster ni modelos descargados. `render-kit.js`, `environment-model.js`, `creature-model.js` y `combat-effects.js` crean geometría, superficies, hojas instanciadas y efectos originales mediante Three.js. Los PNG de `docs/screenshots/20-*` en adelante, `creature-*` y `detail-*` son capturas de WebGL, no imágenes generadas con IA. Las ilustraciones anteriores mantienen su procedencia y función. Véase `docs/MUNDO-3D.md` para distinguir las escenas de inspección de las capturas del juego activo.
