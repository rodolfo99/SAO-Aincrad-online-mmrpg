# Validación del paquete de fuentes Java

Comprobación realizada el 30 de septiembre de 2026, en Linux x86_64 con OpenJDK 17.0.20 y Maven 3.9.9.

Se creó el ZIP de fuentes, se extrajo en una carpeta nueva y se compiló el cliente desde esa extracción, sin copiar directorios `target` de la entrega original. Los fuentes y recursos de `client-java/src/` coinciden byte por byte con la copia original recuperada.

| Comprobación | Resultado |
| --- | --- |
| Fuentes Java de producción | 21 clases incluidas y compiladas |
| Fuentes de prueba | 3 clases incluidas |
| Estilos, geometría y manifiesto de arte | Incluidos, iguales a los originales |
| Recursos compartidos y servidor de contrato | Incluidos en sus rutas originales |
| Integridad del ZIP de fuentes | CRC correcto; 324 archivos iniciales comprobados con SHA-256 |
| Sintaxis de scripts Bash | Correcta |
| `CoreTest` | 18 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `ArtContractTest` | 2 pruebas, 0 fallos, 0 errores, 0 omitidas |
| `ServerContractTest` | 7 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Total del cliente Java | 27 pruebas aprobadas |
| Generación del paquete ejecutable | `mvn package -DskipTests`: BUILD SUCCESS; JAR y dependencias incluidos, ZIP comprobado |

Para la comprobación de fuentes y pruebas se utilizó `./scripts/compilar-cliente-java.sh -o` con dependencias ya disponibles en la caché Maven de este entorno. El empaquetado ejecutable se volvió a generar con `mvn -o -B -f client-java/pom.xml package -DskipTests`, sin repetir las 27 pruebas aprobadas. En tu equipo usa el script sin `-o` en la primera compilación para descargar dependencias.

La versión final del ZIP incorpora este informe y regenera `SHA256SUMS.txt` para todos sus archivos. El archivo de sumas no se incluye a sí mismo en el manifiesto. Los informes de las pruebas se conservan en `docs/validation/java-source-tests.json`.

Este empaquetado no vuelve a ejecutar la partida visual ni los builds de Angular y del servidor, cuyos fuentes se conservaron. La prueba de contrato sí ejecuta siete veces el JAR del servidor incluido con datos temporales. La documentación anterior de las pruebas mixtas permanece en `docs/CLIENTE-JAVA.md`. Siguen pendientes la comprobación en GPU física y las ejecuciones gráficas de Windows/macOS.
