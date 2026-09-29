# Patio de entrenamiento

En el primer piso está centrado en **X 0, Z 27**, al sur del refugio. Abre **◎ Entrenamiento**, pulsa **Ir al patio** y selecciona un muñeco desde el panel o haciendo clic en su modelo 3D. Caminar conserva la velocidad normal; no teletransporta.

| Muñeco | Posición | PV | Defensa | Uso |
|---|---|---:|---:|---|
| Paja | −2, 27 | 5000 | 0 | Daño individual y comparación de armas |
| Acorazado | 2, 27 | 5000 | 12 | Mitigación de golpes y maldiciones |
| Área | 0, 30 | 5000 | 0 | Probar daño a varios objetivos |
| Aliado de prácticas | 0, 24 | 5000 | 0 | Curaciones y mejoras; empieza con 2500 PV |

Espacio usa ataque normal; Q la habilidad de clase; T la racial; 1–4 las habilidades activas aprendidas. Los ataques, maldiciones, daño periódico e invocaciones utilizan las mismas reglas de alcance, recarga y potencia que fuera del patio. La provocación se registra, aunque un muñeco no camina ni ataca. El aliado recibe curaciones y mejoras; se excluye de los ataques ofensivos.

El servidor registra por personaje daño efectivo (limitado a los PV restantes), curación efectiva, impactos, críticos, mejoras/provocaciones, DPS y curación por segundo. El tiempo se mide del primer al último evento, con mínimo de 1 segundo. Incluye daño periódico y daño de tu invocación. Los valores se congelan mientras descansas; una pausa mayor de 15 s inicia un registro nuevo en la próxima acción. **Reiniciar contadores** borra solo tu medición. Los impactos posteriores vuelven a sumarse: cambia al aliado o deja terminar la invocación/maldición antes de iniciar una comparación nueva.

No dan EXP, col, materiales, avance de misión ni bajas PvP. No modifican ciudadanía. El daño letal repone inmediatamente al muñeco ofensivo; tras 5 s sin actividad también se restablecen vida y efectos. El aliado vuelve a mitad de vida para repetir curaciones. Los estados de los muñecos son compartidos entre los jugadores del piso; cada jugador tiene sus propios contadores.

La zona bloquea ataques PvP desde o hacia su interior y los ataques de criaturas. No evita un ataque PvP entre dos jugadores que estén ambos fuera. Los enfriamientos no se reinician al entrar ni al pulsar Reiniciar contadores.

## Configuración root

En el editor de mundo, **Zona de entrenamiento** permite añadir o retirar un patio por piso, nombrarlo, colocarlo, cambiar radio y tiempos y añadir/retirar muñecos. Cada muñeco tiene nombre, posición, PV, defensa y marca de aliado. **Guardar y validar → Aplicar y reconectar** propaga el cambio.

Límites: radio 3–12 m, 1–20 muñecos, 100–100000 PV, defensa 0–100, reposición 1–60 s, pausa de sesión 5–300 s. Los muñecos deben quedar separados 1,5 m y a 1 m del borde. No puede haber edificios, recursos ni apariciones de criaturas dentro de la zona; los árboles decorativos se generan fuera del patio.

Las métricas, vida y efectos de los muñecos son temporales. El mundo guarda sus definiciones. Reiniciar o aplicar el mundo borra las mediciones; mantiene progresión y recargas del personaje.
