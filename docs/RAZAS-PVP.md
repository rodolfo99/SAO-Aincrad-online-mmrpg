# Razas y ciudadanía

Las cinco razas están disponibles para chico/chica y para todas las clases y especialidades. Apariencia, paletas, ojos, geometría, pasivos y habilidad T se configuran desde root. Los retratos son ilustraciones originales; la representación jugable se genera como geometría 3D.

| Raza | Pasivos iniciales | Habilidad T |
|---|---|---|
| Humano | Sin modificador | Recuperar 20 PV; 30 s de recarga |
| Enano | +15 PV, +1 defensa, −0,2 velocidad | Escudo de 30 puntos durante 8 s; 30 s de recarga |
| Elfo | +2 % crítico | +2 velocidad durante 5 s; 25 s de recarga |
| Elfo oscuro | −5 PV, +1 daño, +3 % crítico | +15 % crítico durante 6 s; 30 s de recarga |
| Draconiano | +10 PV, +1 daño, −0,1 velocidad | Aliento: 18 + la mitad del daño, alcance 6; 25 s de recarga |

Los tipos de efecto están implementados en Java. Root puede ajustar parámetros, pero añadir un tipo de mecánica o modelo totalmente nuevo requiere código.

## Reglas PvP iniciales

Activa ataques en **⚖ PvP y ciudadanía** y selecciona un jugador. Desactivarlos evita ataques salientes accidentales; no concede inmunidad fuera de zonas protegidas. La clase, especialidad, género o raza no cambian las reglas de ciudadanía.

Se empieza con 100 puntos. Matar sin justificación resta 25, suma un asesinato y deja marca de **Asesino** durante al menos 600 s. Si la ciudadanía sigue en 50 o menos, la marca continúa. Elegir la especialidad guerrera «asesino» no causa una sanción.

La víctima tiene 60 s para defenderse del atacante concreto sin penalización por esa baja. El atacante original no gana ese derecho porque la víctima responda. Por defecto matar a alguien ya marcado también se penaliza; root puede cambiar esta regla. No hay premios de EXP ni col por matar jugadores.

El refugio (radio 10), portales (radio 4), patio y los primeros 15 s tras aparición bloquean ataques. Se exige nivel 1 y el daño PvP usa multiplicador 0,65, con defensa, alcance y enfriamientos calculados en Java. Las maldiciones respetan las zonas protegidas también en sus impactos posteriores. Las curas/mejoras a otros jugadores en combate PvP están restringidas mientras no existe sistema de grupos.

Un golpe marca combate por 20 s. No se puede cambiar equipo, editar personaje, redimir ciudadanía ni usar portal mientras dura. Desconectarse deja al personaje en el mundo durante el tiempo restante, donde puede recibir daño. Volver recupera ese mismo cuerpo. El reinicio del servidor conserva la marca pendiente.

## Reparación y root

Cerca de Lyra u otro guía, 100 col recuperan 5 puntos, con recarga de 60 s; el temporizador de la marca no se elimina. Root puede editar puntos y retirar la marca explícitamente si la ciudadanía supera el umbral, conservando las cifras históricas. Todas las cifras anteriores son configurables.

Los recibos de bajas y cambios de reputación en `pvp-journal` permiten recuperar exactamente una aplicación ante un perfil guardado antes del evento. Conserva todo `data/` en los respaldos. El sistema es una base funcional; no incluye guerras de gremios, duelos consensuados, ranking ni zonas por facción.
