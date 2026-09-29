# NPC conversacionales: Spring AI y Ollama

El módulo `NpcAi.java` usa **Spring AI 1.1.8**, `OllamaChatModel`, `OllamaApi`, `OllamaChatOptions`, `Prompt` y mensajes tipados. No es un proxy JavaScript ni una llamada ficticia. Las pruebas levantan un servidor HTTP compatible con `/api/chat` y verifican la petición real que emite Spring AI. No se ha ejecutado un LLM real en el entorno de entrega.

## Ollama ya instalado en el mismo equipo

1. Inicia Ollama y ejecuta `ollama list` para conocer el nombre exacto de los modelos instalados.
2. Si necesitas uno, descárgalo explícitamente, por ejemplo `ollama pull qwen3:4b`. El proyecto no descarga modelos al arrancar.
3. Entra como root en `/admin` y busca **Voces de Aincrad**.
4. URL: `http://127.0.0.1:11434`. Modelo: el nombre exacto devuelto por `ollama list`.
5. **Consultar modelos** comprueba `/api/tags`. Configura personalidad y respuesta de respaldo para cada NPC.
6. **Probar con Ollama** utiliza el formulario sin guardarlo. Después activa IA y pulsa **Guardar configuración IA**.
7. En el juego, acércate a Lyra y pulsa E. Aparecerá el campo para hablar con el personaje.

## Ollama dentro de Docker Compose

```bash
./scripts/iniciar-docker.sh --profile ai
# Descarga explícita de un modelo dentro del volumen de Ollama:
docker compose --profile ai exec ollama ollama pull qwen3:4b
docker compose --profile ai exec ollama ollama list
```

En el panel usa `http://ollama:11434`: `localhost` dentro del contenedor `game` apunta al propio contenedor, no a Ollama. El servicio Ollama no publica un puerto al exterior.

GPU NVIDIA, si ya tienes NVIDIA Container Toolkit funcionando:

```bash
docker compose -f compose.yaml -f compose.gpu.yaml --profile ai up --build -d
```

Esta variante no se ejecutó en el entorno de entrega. El archivo base usa CPU. Para aprovechar una instalación ROCm/CUDA ya configurada en el host, puedes ejecutar Java directamente junto a Ollama, o configurar una dirección accesible del host. `host.docker.internal` solo sirve si Ollama escucha en una interfaz accesible desde Docker; un servicio enlazado exclusivamente a `127.0.0.1` del host no es accesible por la dirección del gateway. No expongas Ollama sin necesidad a redes externas.

## Configuración persistente

`data/npc-ai.json` se crea al primer inicio y se guarda con copia previa en `data/backups/`. En Docker se encuentra en `game-data`.

| Campo | Función / límite |
|---|---|
| `enabled` | Activación global, inicialmente false |
| `baseUrl` | URL HTTP(S), host y puerto; sin rutas, credenciales o parámetros |
| `model` | Modelo predeterminado; ejemplo inicial `qwen3:4b` |
| `temperature` | 0–2 |
| `maxTokens` | 16–1024, enviado como `num_predict` |
| `contextTokens` | 512–16384, enviado como `num_ctx` |
| `timeoutSeconds` | 1–120, plazo de lectura; conexión acotada a 5 s |
| `historyTurns` | 0–12 turnos previos por jugador y NPC |
| `maxConcurrent` | 1–4 solicitudes concurrentes, compartidas con pruebas admin |
| `maxInputChars` / `maxReplyChars` | Límites de entrada y salida |
| `cooldownSeconds` | 1–60 s entre consultas por jugador |
| `bindings[].npcId` | ID real del NPC en el mundo aplicado |
| `bindings[].enabled` | Activa/desactiva ese vínculo |
| `bindings[].model` | Modelo alternativo; vacío hereda el global |
| `bindings[].systemPrompt` | Personalidad y estilo, hasta 6000 caracteres |
| `bindings[].fallback` | Respuesta si no hay modelo, conexión o capacidad |

La memoria está en RAM y se separa por ID de jugador y NPC. Se elimina al desconectar o al cambiar la configuración y no se guarda en perfiles. No hay streaming token a token; llega una respuesta completa. Un modelo con razonamiento puede consumir sus tokens antes de responder: ajusta modelo y presupuesto desde el panel.

## Límites del módulo

- El servidor verifica que el NPC exista en el piso y que el jugador esté a menos de cuatro unidades.
- Solo una petición pendiente por jugador, con enfriamiento y concurrencia global acotada.
- La llamada se ejecuta en un pool separado del tick de simulación.
- Ollama recibe personalidad, historial limitado y contexto de juego de solo lectura; no recibe contraseña de root ni claves de personaje.
- No se registran herramientas ni funciones con efectos sobre el juego. Un texto como «te doy mil monedas» no modifica el inventario ni la economía.
- Respuestas interpoladas como texto por Angular, sin HTML del modelo.
- Las misiones y el herrero siguen usando reglas Java aunque la IA esté desactivada.

Referencias de implementación: [Spring AI / Ollama](https://docs.spring.io/spring-ai/reference/1.1/api/chat/ollama-chat.html), [compatibilidad Spring AI](https://github.com/spring-projects/spring-ai), [API de Ollama](https://docs.ollama.com/api).
