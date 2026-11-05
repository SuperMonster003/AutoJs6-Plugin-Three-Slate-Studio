3-Slate Studio es el complemento de vídeo y audio de AutoJs6 y también una aplicación de edición que funciona sin AutoJs6. Recorta, une, acelera, escala, gira, encuadra y marca con agua los vídeos, extrae fotogramas, crea GIF y transcodifica; recorta, une, mezcla, aplica fundidos y convierte el audio; y su editor de línea de tiempo multipista combina clips con transiciones, superposiciones y subtítulos.

> Esta es una versión preliminar de desarrollo: el centro de complementos de AutoJs6 puede detectar el complemento, pero las funciones de procesamiento, las pantallas de Studio y los métodos de procesamiento de los módulos `video` y `audio` aún no están disponibles. El texto siguiente describe los objetivos de diseño de cada versión; consulte la hoja de ruta para ver el progreso.

### Funciones Destacadas

- Edición de vídeo: recorte por tiempo, unión, cambio de velocidad (de 0.25x a 8x), escalado, giro, encuadre y volteo, marcas de agua de texto o imagen, extracción o eliminación de la pista de audio y transcodificación (H.264 por defecto, H.265 si el dispositivo lo admite).
- Fotogramas y GIF: fotogramas sueltos, secuencias de fotogramas y miniaturas como archivos de imagen u objetos `Image`, y animaciones GIF con el codificador integrado.
- Audio: recorte, unión, mezcla de varias entradas, ajuste del volumen con fundido de entrada y de salida, cambio de velocidad y conversión a M4A, WAV, FLAC u OGG.
- Línea de tiempo multipista: pistas de vídeo, audio y superposición, 7 transiciones (corte, fundido, disolución, deslizamiento, barrido, zoom, desenfoque) y subtítulos con importación y exportación de SRT y un subconjunto básico de ASS, incrustados en la imagen al exportar.
- Generación de código: cada pantalla de herramienta y la exportación de la línea de tiempo producen el código `video.*` / `audio.*` equivalente, listo para copiar o abrir como un script nuevo en AutoJs6.
- Grabadora de Studio: graba el micrófono, el audio interno del dispositivo (Android 10 o posterior) o ambos desde las pantallas del complemento.
- Biblioteca FFmpeg (1.1.0): una descarga opcional para salida MP3, WebM y MKV, paso directo de comandos con `video.ffmpeg` y renderizado completo de ASS.

El progreso y el diseño están disponibles en la [página del proyecto](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio).
