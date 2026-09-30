# Estado de v0.2.0

## Rutas autoritativas de clic · 29 de septiembre de 2026

Los fuentes añaden A* y una cola de puntos intermedios en Java, con las mismas colisiones circulares, margen del jugador, velocidad y validaciones del mundo. Se conservan `move`, `input`, `stop`, WASD y la cancelación al salir, morir o usar portales. Las pruebas cubren obstáculos, destinos encerrados, velocidad y WebSocket real. Hay un límite de 8192 nodos por búsqueda y una malla de 1 m; pueden omitirse desvíos por pasos estrechos. Recompila para activar la mejora en el JAR. [Uso, alcance y pruebas](MOVIMIENTO-POR-CLIC.md).

Semilla funcional con Java/Spring Boot, Angular/Three.js, root configurable al primer inicio y NPC mediante Spring AI/Ollama. Incluye cinco razas, ocho especialidades, 48 talentos, 33 habilidades activas, 30 conjuntos de ropa/armadura y 30 juegos de armas.

PvP y ciudadanía persistentes; herrero, sastre, minería y leñador; 84 recetas, nueve materiales, siete tipos de recurso, 22 nodos compartidos; herrería, sastrería y tienda. Patio configurable con cuatro muñecos, curaciones/mejoras, daño en área, invocaciones y contadores de práctica. Nueve ilustraciones originales y geometría 3D local.

Bestiario configurable: siete modelos 3D, cinco zonas de niveles 1–20 y 26 criaturas nuevas. Root edita especies, crecimiento, botín, población, posiciones y reaparición con vista previa. Los enemigos respetan territorio, regresan al origen y muestran nivel; el mundo usa esquema 7 con migración de las entregas anteriores.

La entrega incluye fuentes, JAR, Angular compilado, configuración de Maven/Node.js y Docker Compose, scripts y documentación. Maven y Node.js se instalan por separado para recompilar. Consulta VALIDACION.md y los registros de navegador para los resultados y sus límites. Es una base de dos pisos para seguir ampliando, sin pruebas de escala masiva.

Revisión visual de personajes: nueva anatomía y rostro, pelo por mechones, materiales físicos, ropa por capas y extremidades articuladas. Vista de rostro/cuerpo giratoria y apariencias propias de Lyra y comerciantes; se mantiene la personalización de razas, género y equipo. El acabado sigue siendo estilizado y no alcanza el fotorrealismo de un modelo artístico con texturas de alta resolución. Detalles y controles en PERSONAJES-3D.md.

## Revisión visual integral · 29 de septiembre de 2026

Entorno, edificios, árboles, recursos, bestiario, muñecos, invocaciones y efectos renovados con geometría suave y materiales propios. Calidades Ligera/Equilibrada/Alta en el cliente, lotes espaciales, hojas instanciadas y liberación de mapas de sombra. Se corrigieron también los círculos visibles sin selección y el picking sobre partes ocultas de recursos. Esquema 7 y servidor sin cambios. Detalles y límites en [MUNDO-3D.md](MUNDO-3D.md).

## Ampliación de los dos pisos · 29 de septiembre de 2026

Radios 92/84 m y diámetros 184/168 m: cuatro veces la superficie anterior. Zonas de caza redistribuidas, 255/225 elementos de vegetación, 22 recursos y portales alejados. Sendero, plaza, entorno y minimapa adaptan sus dimensiones al piso. Root muestra diámetro y área. El actualizador con copia previa conserva catálogos y datos de personajes y se detiene ante distribuciones personalizadas. Consulta [PISOS-AMPLIADOS.md](PISOS-AMPLIADOS.md).

## Cuentas e inicio de sesión obligatorio · 29 de septiembre de 2026

Registro con usuario/contraseña y confirmación, sesiones con cookie HttpOnly, selección de varios personajes por cuenta y recuperación desde otro navegador. El servidor rechaza conexiones anónimas y personajes ajenos, y desconecta al cerrar sesión, caducar o cambiar/restablecer la contraseña. Se conservan perfiles anteriores con vinculación autenticada o recuperación root. Root gestiona contraseñas y propietarios; el registro público y la duración/cookie de sesión se configuran por entorno y Compose. No se añaden dependencias de base de datos. [CUENTAS.md](CUENTAS.md).

## Correo y mercado

Recuperación por correo con enlaces de un uso y caducidad, notificación del restablecimiento y gestión del correo propio. Utilidad Postfix por IP en Compose, con comandos de configuración, prueba, cola y diagnóstico; sin dominio ni cuenta SMTP externos obligatorios. El intento a Hotmail no pudo conectar por la restricción de red del entorno.

Bolsa de recursos con venta parcial/completa al mercader, precios de reventa root y guardado atómico. Catálogo de 28 productos básicos: compra de armas principales/secundarias y ropa/armaduras por clase, género y nivel; equipo individual, reventa y mejora cuando tenga receta. Root añade, modifica o retira productos.

## Publicación en GitHub · 29 de septiembre de 2026

Fuentes y compilados v0.2.0 publicados en `main` de [SAO-Aircraft-online-mmrpg](https://github.com/rodolfo99/SAO-Aircraft-online-mmrpg). Se comprobó la coincidencia de los 255 archivos de la subida con el ZIP de entrega. La documentación posterior completa [arranque y puertos](ARRANQUE-Y-PUERTOS.md), [publicación y actualización](PUBLICACION.md) y los enlaces de operación y del perfil de GitHub.

Esta revisión documental conserva el código y la versión v0.2.0. La partida completa en Ubuntu 26.04 está realizada, según la confirmación del usuario del 29 de septiembre de 2026. Siguen pendientes la carga multijugador masiva y la entrega de correo a proveedores externos. Consulta [VALIDACION.md](VALIDACION.md) para el registro de la validación manual y las comprobaciones automatizadas.
