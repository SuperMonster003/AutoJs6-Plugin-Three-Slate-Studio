3-Slate Studio es el complemento de vídeo y audio de AutoJs6 y también una aplicación de edición que funciona sin AutoJs6. Recorta, une, acelera, escala, gira, encuadra y marca con agua los vídeos, extrae fotogramas, crea GIF y transcodifica; recorta, une, mezcla, aplica fundidos y convierte el audio; y su editor de línea de tiempo multipista combina clips con transiciones, superposiciones y subtítulos.

> La versión 1.0.0 es la primera versión publicada. Procesar medios en scripts requiere AutoJs6 6.8.1 (compilación interna 6641) o posterior; las pantallas del complemento, el editor de línea de tiempo y la grabadora de Studio no dependen de AutoJs6.

### Funciones Destacadas

- Edición de vídeo: recorte por tiempo, unión, cambio de velocidad (de 0.25x a 8x), escalado, giro, encuadre y volteo, marcas de agua de texto o imagen, extracción o eliminación de la pista de audio y transcodificación (H.264 por defecto, H.265 si el dispositivo tiene un codificador H.265).
- Fotogramas y GIF: fotogramas sueltos, secuencias de fotogramas y miniaturas como archivos PNG o JPG u objetos `Image`, y animaciones GIF con el codificador integrado.
- Audio: recorte, unión, mezcla de varias entradas (reducida automáticamente cuando la suma saturaría), ajuste del volumen con fundido de entrada y de salida, cambio de velocidad y conversión a M4A, WAV, FLAC u OGG (OGG requiere Android 10 o posterior).
- Editor de línea de tiempo multipista: pistas de vídeo, audio, superposición y subtítulos, 7 transiciones (corte, fundido, disolución, deslizamiento, barrido, zoom, desenfoque), vista previa en directo, deshacer y rehacer, y proyectos guardados como `.slate.json`; los subtítulos admiten importación y exportación de SRT y un subconjunto básico de ASS, incrustados en la imagen al exportar.
- Caja de herramientas: un asistente para cada función de procesamiento, con formularios que muestran los valores por defecto y los rangos; las tareas se ejecutan en una cola que muestra el progreso y el motivo de los fallos, y las tareas largas mantienen una notificación en primer plano.
- Generación de código: cada asistente, la exportación de la línea de tiempo y la grabadora producen el script `video.*` / `audio.*` equivalente, listo para copiar o abrir como un script nuevo en AutoJs6.
- Grabadora de Studio: graba el micrófono, el audio interno del dispositivo (Android 10 o posterior) o ambos desde las pantallas del complemento en M4A o WAV, con pausa y una grabación que continúa con la pantalla apagada.
- Biblioteca FFmpeg (prevista para la 1.1.0): una descarga opcional que añade formatos como MP3, WebM y MKV y el renderizado completo de subtítulos ASS.

El progreso y el diseño están disponibles en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio).
