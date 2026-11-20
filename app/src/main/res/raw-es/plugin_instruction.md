3-Slate Studio es una aplicación de edición de video y audio que también funciona sola, sin AutoJs6. Recorta, une, acelera y transcodifica videos, añade marcas de agua o crea GIF; también recorta, mezcla y convierte audio, y monta varios clips en un solo video en una línea de tiempo, con transiciones y subtítulos. Con AutoJs6, los scripts realizan el mismo procesamiento mediante los módulos `video` y `audio`.

### Primeros pasos

1. Abre 3-Slate Studio. Mientras no se haya concedido el acceso, "Biblioteca" muestra "Conceder acceso"; tócalo y permite el acceso a todos los archivos en la página del sistema que se abre.
2. En "Herramientas", elige una operación, por ejemplo "Recortar video", selecciona el medio con "Explorar archivos" o "Desde la biblioteca", ajusta las opciones y toca "Iniciar".
3. Durante el procesamiento se muestra el progreso; al terminar aparecen los archivos de salida, la información del medio y el "Script equivalente". Todas las tareas también aparecen en "Tareas".
4. Antes de usarlo en scripts, comprueba que este complemento esté activado en el "Centro de plugins" de AutoJs6.

### Funciones principales

- **Herramientas**: cada operación tiene su propio asistente, por ejemplo "Recortar video", "Añadir texto e imágenes", "Convertir a GIF" y "Mezclar audio"; los formularios muestran los valores predeterminados y los rangos permitidos.
- **Edición en línea de tiempo**: toca "Nuevo proyecto de edición" en "Herramientas" para colocar clips en las pistas de video, audio, superposiciones y subtítulos, añadir transiciones, previsualizar en tiempo real y deshacer o rehacer; "Exportar" envía el resultado a la cola de tareas.
- **Subtítulos**: en la línea de tiempo, "Importar subtítulos" lee archivos SRT o ASS, que se incrustan en la imagen al exportar; "Herramientas" también ofrece "Incrustar subtítulos" y "Convertir subtítulos".
- **Grabación de audio**: toca el botón "Grabar audio" de la barra superior de la pantalla principal o "Grabar audio" en "Herramientas" para grabar M4A o WAV desde "Micrófono", "Audio interno" o "Mixto"; la grabación se puede pausar y continúa con la pantalla apagada.
- **Script equivalente**: los resultados de los asistentes, de las exportaciones de la línea de tiempo y de las grabaciones incluyen un "Script equivalente", que puedes "Copiar" o "Abrir en AutoJs6" como un script nuevo sin guardar.
- **Biblioteca y tareas**: "Biblioteca" muestra los medios de las carpetas de salida y de la carpeta de grabaciones de AutoJs6, que puedes previsualizar, compartir, renombrar o "Procesar con las herramientas"; "Tareas" muestra el progreso y el motivo de error de cada tarea, y permite cancelarla.
- **Ajustes**: "Ajustes" en la barra superior permite cambiar la apariencia, la "Carpeta de salida", el "Códec de video predeterminado", las "Tareas simultáneas", etc.; la "Carpeta de salida" solo afecta a los asistentes de las herramientas, y los scripts mantienen sus propias salidas predeterminadas.

### Uso en scripts

En los scripts de AutoJs6, el módulo global `video` procesa videos. El ejemplo siguiente toma 5 segundos de `./video.mp4` a partir del segundo 2, muestra el progreso y convierte el fragmento a GIF:

```js
let clip = video.trim("./video.mp4", {
    start: 2000,
    duration: 5000,
    onProgress: (p) => console.log(p.stage, p.percent),
});
let gif = video.toGif(clip.path, { fps: 10, width: 480 });
console.log(gif.path, gif.sizeBytes);
```

El módulo `audio` procesa sonido. El ejemplo siguiente mezcla una voz con música de fondo a menor volumen y luego convierte el resultado a FLAC:

```js
let mixed = audio.mix([
    { src: "./voice.m4a" },
    { src: "./music.m4a", volume: 0.3 },
]);
let flac = audio.convert(mixed.path, { format: "flac" });
console.log(flac.path, flac.durationMillis);
```

- Cada método de procesamiento tiene también una forma `Async` que devuelve una Promise, por ejemplo `video.trimAsync`; las formas síncronas no se pueden llamar en el hilo de la UI. Detener un script cancela sus tareas sin terminar.
- Las rutas relativas se resuelven respecto al directorio de trabajo del script. Si se omite la ruta de salida, los resultados se guardan según su tipo en la carpeta 3-Slate Studio de `Movies`, `Music` o `Pictures`.
- La grabación de pantalla con `video.record` y la grabación de audio con `audio.record` las realiza AutoJs6 y no necesitan este complemento; las composiciones multipista usan `video.compose`, que comparte el formato de proyecto de la edición en línea de tiempo.
- Todos los métodos y opciones se describen en las secciones [video](https://docs.autojs6.com/#/video) y [audio](https://docs.autojs6.com/#/audio) de la documentación de AutoJs6.

### Requisitos

- Android 7.0 o posterior. Las pantallas del complemento, la edición en línea de tiempo y la grabación de audio no necesitan AutoJs6.
- El procesamiento en scripts requiere AutoJs6 6.8.1 o posterior, con este complemento activado en el "Centro de plugins".
- El acceso a todos los archivos se usa para leer y escribir los archivos indicados por los scripts y las pantallas, así como las carpetas de salida predeterminadas.
- El permiso de micrófono se usa para grabar en las pantallas del complemento (el audio interno también lo necesita); "Audio interno" y "Mixto" requieren Android 10 o posterior, y el sistema te pide antes confirmar la transmisión de la pantalla.
- El permiso de notificaciones se usa para mostrar el progreso de las tareas largas y los controles de grabación en el panel de notificaciones.

### Preguntas frecuentes

**Motivos por los que se necesita el acceso a todos los archivos**

El complemento lee y escribe directamente los archivos en las rutas indicadas por los scripts o las pantallas, y las carpetas de salida predeterminadas están en el almacenamiento compartido. Sin ese acceso, el procesamiento devuelve `STORAGE_PERMISSION_DENIED` y abre la página de permisos.

**Casos en que la exportación de la línea de tiempo devuelve `CODEC_ERROR`**

Un dispositivo solo puede usar un número limitado de códecs a la vez, y las transiciones superpuestas decodifican dos videos al mismo tiempo. Usa menos pistas de video o transiciones superpuestas, o cambia "Tareas simultáneas" a 1 en "Ajustes".

**Casos en que las tareas largas se retrasan o la grabación se detiene con la pantalla apagada**

La optimización de batería o las restricciones en segundo plano pueden retrasar las tareas largas o detener la grabación con la pantalla apagada. Toca "Uso de batería" en "Ajustes" y, en la página de información de la aplicación que se abre, elige Sin restricciones (o Permitir actividad en segundo plano) para la batería.

**Motivos por los que algunas aplicaciones quedan en silencio en el audio interno**

Una aplicación puede prohibir que otras capturen el sonido que reproduce, y esas aplicaciones quedan en silencio en el audio interno. Usa "Micrófono" para grabar esos sonidos.

Los detalles de las pantallas, los permisos y los scripts equivalentes están en la sección [3-Slate Studio](https://docs.autojs6.com/#/threeSlateStudio) de la documentación de AutoJs6.
