# Clases, atributos y árboles de talentos

Las clases iniciales son **guerrero, mago y sanador**. Sus reglas son una elección de diseño de esta semilla; no pretenden describir las reglas canónicas de SAO.

| Clase | Vida / daño / velocidad base | Ataque normal | Habilidad Q |
|---|---|---|---|
| Guerrero | 100 / 19 / 5.5 | Espada, alcance 3.1 | Corte celeste, ×2, alcance 4.4, cada 4 s |
| Mago | 85 / 17 / 5.6 | Bastón, alcance 10 | Descarga arcana, ×2.2, alcance 13, cada 5 s |
| Sanador | 110 / 12 / 5.2 | Vara de luz, alcance 8 | Luz compartida, cura aliados vivos y heridos a 8 unidades, cada 6 s |

Los valores se editan en root. El sanador cura a sí mismo y a todos los aliados cercanos del mismo piso; no revive. La curación base es `30 + daño_base × 0.5 + efectos healing`, redondeada y limitada a 1–250 por objetivo, sin superar la vida máxima. No consume enfriamiento si nadie necesita curación. Quien cura a un participante de un combate cercano queda incluido en la recompensa, sujeto a estar vivo y cerca al derrotar al enemigo.

## Repartir puntos como personaje

1. Gana EXP derrotando criaturas o completando la misión. Con la configuración inicial, cada 100 EXP ganas un nivel, 3 puntos de atributo y 1 de talento. Nivel máximo: 100.
2. Regresa al refugio y espera a que no queden criaturas a menos de 8 m; el refugio bloquea PvP, pero una criatura puede perseguirte hasta su límite de territorio. Pulsa **✧ Atributos y talentos**. Los puntos disponibles aparecen junto al botón y dentro del panel.
3. Usa **＋** en un atributo o talento. Cada nodo muestra nivel requerido, rangos, coste y predecesores. Un requisito exige al menos un rango de cada predecesor.
4. Pulsa **Guardar atributos y talentos**. El servidor valida y guarda todo el reparto de una vez.

No basta cambiar el HTML, un contador o los datos de red: el presupuesto se calcula con el nivel guardado en Java. El reparto no aumenta la vida actual; al reducir vida máxima se limita la actual. Los efectos de equipo, atributos y talentos se suman.

| Atributo | Efecto por punto | Rangos iniciales |
|---|---|---|
| Vitalidad | +5 vida máxima | 50 |
| Poder | +1 daño base | 50 |
| Agilidad | +0.04 velocidad, +0.15 % crítico | 50 |
| Espíritu | +2 curación, −0.05 s de enfriamiento de habilidad | 50 |

Además existe crecimiento automático configurable de +15 vida y +2 daño por nivel. Root puede reducirlo a cero si prefiere que toda la progresión venga del reparto manual.

## Árboles iniciales

Cada clase tiene un fundamento de nivel 2 y dos ramas, con nodos de niveles 3 y 5. Los 15 talentos pasivos iniciales tienen tres rangos y coste 1 por rango. Se añaden 33 talentos de un rango para desbloquear habilidades activas (48 en total), con ramas de niveles 2 → 4 → 6. Puedes combinar ramas dentro del presupuesto.

| Clase | Fundamento (nivel 2) | Primera rama (niveles 3 → 5) | Segunda rama (niveles 3 → 5) |
|---|---|---|---|
| Guerrero | Disciplina: daño | Baluarte: defensa → Veterano: vida | Furia: crítico → Filo del cielo: potencia Q |
| Mago | Concentración: daño | Runas ardientes: potencia Q → Estrella arcana: crítico | Flujo mágico: enfriamiento → Velo arcano: vida y defensa |
| Sanador | Gracia: curación | Amparo: defensa → Santuario: vida | Renovación: enfriamiento → Resplandor: daño y curación |

## Configurar desde root

**Clases, atributos y árboles** permite configurar EXP por nivel, nivel máximo, puntos ganados, bonos automáticos y redistribución. Los desplegables de cada habilidad permiten editar nombre, tipo, alcance, potencia y enfriamiento. Para Q, los tipos disponibles son `strike`, `arcane` y `heal`; las nuevas habilidades activas también admiten daño, maldición, mejora, curación, provocación e invocación; añadir una mecánica completamente nueva requiere código Java y visuales del cliente.

**Nuevo atributo de personaje** y **Nuevo talento** crean definiciones editables. Define nombre, descripción, rangos y efectos; en talentos, clase, rama, nivel, coste y lista de IDs predecesores. El servidor rechaza ciclos, IDs desconocidos, enlaces entre clases, predecesores de nivel superior y valores fuera de límites. Efectos soportados: `maxHp`, `damage`, `defense`, `speed`, `criticalChance`, `healing`, `skillPower`, `cooldownReduction`, `none` (solo descriptivo).

Después, **Guardar y validar → Aplicar y reconectar**. El reparto de un perfil que resulte incompatible con el nuevo catálogo se restablece: vuelve a disponer de los puntos que le corresponden por nivel, conservando EXP, monedas e identidad. Guarda respaldo antes de cambiar las reglas de una partida avanzada.

**Personajes del mundo** permite a root modificar EXP y distribuir los puntos de cualquier personaje, incluso desconectado. El árbol usa el nivel calculado con la EXP del formulario. Guarda con **Guardar personaje**. Se mantiene el mismo presupuesto y las mismas dependencias; root puede aumentar la EXP o las reglas de puntos. Al cambiar de clase en el editor root, pulsa **Devolver talentos** antes de guardar y asigna talentos de la nueva clase. Cada edición administrativa crea respaldo.

Si `allowRespec` está activo, el jugador puede retirar puntos o restablecer el reparto en el refugio. Quitar un predecesor desde el formulario devuelve también los nodos dependientes. Cambiar de clase devuelve talentos de la clase anterior, conservando los atributos compatibles. Si está desactivado, el jugador solo puede añadir rangos y no cambiar de clase; root conserva sus facultades.

## Límites y persistencia

`Progression.java` valida catálogos y repartos. Los perfiles guardan `attributeRanks` y `talentRanks`; perfiles anteriores sin estos campos empiezan con todos sus puntos disponibles. La EXP se limita a 100000. No hay mana, lanzamiento canalizado, selección manual de objetivo aliado, PvP, especializaciones exclusivas ni talentos que concedan nuevas teclas en esta versión.

Límites finales: vida 30–500, daño base 1–150, defensa 0–40, velocidad 2–10 y crítico 0–50 %. La potencia de habilidades ofensivas se limita a ×5 y el enfriamiento nunca baja de 1 s. Los límites y tipos de efectos ejecutables están explícitos en Java; las cantidades y árboles se configuran en `world.json`.

## Especialidades y habilidades activas

La selección de especialidad se hace al crear personaje y en Mi personaje, dentro del refugio. Root también puede modificarla. Cada talento puede limitarse a una o varias especialidades de su clase. Al cambiar clase o especialidad se devuelve el reparto de talentos; el servidor lo impide para el jugador si root desactivó la redistribución. No aumenta la vida actual.

Las habilidades desbloqueadas se muestran en **Habilidades activas** y se asignan a teclas 1–4. Los primeros cuatro desbloqueos se asignan automáticamente si hay espacios libres. Recargas, alcance, área y objetivos se resuelven en Java. Los datos de las ramas están en [ESPECIALIDADES.md](ESPECIALIDADES.md).
