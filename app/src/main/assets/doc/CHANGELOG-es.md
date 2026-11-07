******

### Historial De Versiones

******

# v1.0.0

###### 2026/11/08

* `Aviso` Procesar medios con video / audio en scripts requiere AutoJs6 6.8.1 (compilación interna 6641) o posterior; las pantallas del complemento, el editor de línea de tiempo y la grabadora de Studio no dependen de AutoJs6
* `Aviso` La API de script se describe en los capítulos [video](https://docs.autojs6.com/#/video) y [audio](https://docs.autojs6.com/#/audio) de la documentación de AutoJs6
* `Función` Procesamiento de vídeo: recorte, unión, velocidad (0.25x - 8x), escalado, giro, encuadre y volteo, marcas de agua de texto o imagen, extracción o eliminación de la pista de audio y transcodificación (H.264, o H.265 si el dispositivo lo admite)
* `Función` Fotogramas, miniaturas y GIF: archivos PNG / JPG u objetos Image, y un codificador GIF integrado (paleta, tramado, bucle)
* `Función` Procesamiento de audio: recorte, unión, mezcla (reducida automáticamente si saturaría), volumen y fundidos, velocidad y conversión a M4A / WAV / FLAC / OGG (OGG requiere Android 10+)
* `Función` Composición multipista con video.compose: hasta 8 pistas de vídeo y 16 de audio, 7 transiciones, superposiciones de texto / imagen y subtítulos
* `Función` Subtítulos: lectura y escritura de SRT y un subconjunto básico de ASS, incrustados en la imagen con video.burnSubtitles, y video.subtitles.parse / write para archivos de subtítulos
* `Función` Módulos de script de AutoJs6 video / audio (AutoJs6 6.8.1, Rhino, Node.js y Bun): cada método de procesamiento tiene una forma xxxAsync y una devolución de llamada de progreso, y detener el script cancela sus tareas
* `Función` Pantallas de Studio: biblioteca multimedia, caja de herramientas, cola de tareas y ajustes; la apariencia sigue a AutoJs6 o usa su propio idioma, modo nocturno y color de tema, y los ajustes guían para conceder el acceso a todos los archivos y el uso de batería sin restricciones
* `Función` Asistentes de la caja de herramientas: un asistente por función de procesamiento, con formularios que muestran los valores por defecto y los rangos, y una página de resultado con la salida, la información del medio y el script equivalente
* `Función` Editor de línea de tiempo: edición multipista, transiciones, edición de superposiciones y subtítulos, vista previa en directo, deshacer / rehacer, proyectos guardados como .slate.json y exportación a la cola de tareas
* `Función` Generación de código: los asistentes, la línea de tiempo y la grabadora muestran el script completo equivalente, listo para copiar o abrir en AutoJs6 como un script nuevo
* `Función` Grabadora de Studio: micrófono, audio interno del dispositivo (Android 10+) o ambos, M4A / WAV, pausa / reanudación, grabación con la pantalla apagada y controles en la notificación
* `Función` Las tareas largas se ejecutan en un servicio en primer plano con una notificación de progreso; los ajustes eligen el codificador de hardware preferido y cuántas tareas se ejecutan a la vez (1 - 2)
* `Función` Las salidas sin ruta van a las carpetas 3-Slate Studio dentro de Movies, Music y Pictures y se registran en la biblioteca multimedia
* `Función` Búsqueda de actualizaciones: lee el índice oficial de complementos como máximo una vez cada 12 horas al abrir la pantalla de inicio, y se puede ignorar una versión
* `Función` Un único APK para todas las ABI: la única biblioteca nativa procede de Jetpack Compose y está alineada a páginas de memoria de 16 KB
* `Función` Pantallas, README, instrucciones del centro de complementos y registro de cambios en 10 idiomas
* `Dependencia` Se añade Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer y UI, para el procesamiento y las vistas previas)
* `Dependencia` Se añade Jetpack Compose BOM 2026.09.00 (pantallas del complemento)
* `Dependencia` Se añade AndroidX Activity Compose 1.13.0
* `Dependencia` Se añade Navigation Compose 2.10.2
* `Dependencia` Se añade AndroidX Core KTX 1.18.0
* `Dependencia` Se añade Material Color Utilities (MaterialKolor) 4.1.1 (color de tema de las pantallas del complemento)
