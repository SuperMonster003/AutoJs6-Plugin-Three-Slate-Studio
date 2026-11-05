<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Procesamiento de vídeo y audio, edición multipista con línea de tiempo, transiciones y subtítulos, con código de script generado desde las pantallas</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Idiomas (Languages)

******

El README.md actual admite los siguientes idiomas:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- Español [es] # actual
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### Introducción

******

3-Slate Studio es el complemento de vídeo y audio de AutoJs6 y también una aplicación de edición que funciona sin AutoJs6. Recorta, une, acelera, escala, gira, encuadra y marca con agua los vídeos, extrae fotogramas, crea GIF y transcodifica; recorta, une, mezcla, aplica fundidos y convierte el audio; y su editor de línea de tiempo multipista combina clips con transiciones, superposiciones y subtítulos.

El procesamiento se ejecuta en el complemento con Jetpack Media3 Transformer y los códecs por hardware del dispositivo, de modo que el host AutoJs6 sigue siendo ligero. Los scripts de AutoJs6 procesan medios con los módulos globales `video` y `audio`; la grabación de pantalla (`video.record`) y la grabación de audio (`audio.record`) están integradas en AutoJs6 y funcionan sin el complemento. Cada pantalla de herramienta del complemento muestra el código de script equivalente, que se puede copiar o abrir como un script nuevo en AutoJs6.

> Esta es una versión preliminar de desarrollo: el centro de complementos de AutoJs6 puede detectar el complemento, pero las funciones de procesamiento, las pantallas de Studio y los métodos de procesamiento de los módulos `video` y `audio` aún no están disponibles. El texto siguiente describe los objetivos de diseño de cada versión; consulte la hoja de ruta para ver el progreso.

******

### Funciones Destacadas

******

- Edición de vídeo: recorte por tiempo, unión, cambio de velocidad (de 0.25x a 8x), escalado, giro, encuadre y volteo, marcas de agua de texto o imagen, extracción o eliminación de la pista de audio y transcodificación (H.264 por defecto, H.265 si el dispositivo lo admite).
- Fotogramas y GIF: fotogramas sueltos, secuencias de fotogramas y miniaturas como archivos de imagen u objetos `Image`, y animaciones GIF con el codificador integrado.
- Audio: recorte, unión, mezcla de varias entradas, ajuste del volumen con fundido de entrada y de salida, cambio de velocidad y conversión a M4A, WAV, FLAC u OGG.
- Línea de tiempo multipista: pistas de vídeo, audio y superposición, 7 transiciones (corte, fundido, disolución, deslizamiento, barrido, zoom, desenfoque) y subtítulos con importación y exportación de SRT y un subconjunto básico de ASS, incrustados en la imagen al exportar.
- Generación de código: cada pantalla de herramienta y la exportación de la línea de tiempo producen el código `video.*` / `audio.*` equivalente, listo para copiar o abrir como un script nuevo en AutoJs6.
- Grabadora de Studio: graba el micrófono, el audio interno del dispositivo (Android 10 o posterior) o ambos desde las pantallas del complemento.
- Biblioteca FFmpeg (1.1.0): una descarga opcional para salida MP3, WebM y MKV, paso directo de comandos con `video.ffmpeg` y renderizado completo de ASS.

******

### Uso

******

1. Descargue el APK desde [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) e instálelo. Hay un único paquete para todas las ABI.
2. Abra el complemento y conceda el acceso a todos los archivos cuando se solicite, para que pueda leer y escribir las rutas que elija.
3. Para usar los métodos de procesamiento de `video` y `audio` en scripts, actualice AutoJs6 a la compilación interna 6641 (6.8.1) o posterior y active el complemento en el centro de complementos.

> El complemento es compatible con dispositivos con Android 7.0 (API 24) o posterior. Su única biblioteca nativa procede de Jetpack Compose y está alineada a páginas de memoria de 16 KB, por lo que un único paquete sirve para todas las ABI.

******

### API De Script

******

Los scripts usan estas funciones mediante los módulos globales `video` y `audio` (también disponibles como `$video` y `$audio`) (borrador de diseño de la 1.0.0; prevalece la documentación de la versión publicada):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }], null);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });

let gif = video.toGif(joined.path, { fps: 10, width: 480 });
toastLog(gif.path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }], null);

let recording = video.record(null, { audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- Cada método de procesamiento tiene una forma bloqueante y una forma `xxxAsync` que devuelve una Promise; `options.onProgress(progress)` informa de `percent`, `stage` y `etaMillis`, y detener el script cancela sus tareas.
- Si se omite `output`, los vídeos van a `Movies/3-Slate Studio/` y el audio a `Music/3-Slate Studio/`, con un nombre formado por el origen, la operación y la hora; los resultados incluyen la ruta real y la información del medio y se registran en la biblioteca multimedia.
- `video.compose(timeline)` renderiza una línea de tiempo multipista descrita como un objeto (pistas, clips, transiciones, superposiciones, subtítulos), el mismo formato que exporta el editor de línea de tiempo.
- `video.tasks()` / `audio.tasks()` enumeran y cancelan las tareas en curso del script; los fallos lanzan un `MediaStudioError` con un `code` estable como `INVALID_OPTIONS` o `STORAGE_PERMISSION_DENIED`.

******

### Preguntas Frecuentes

******

#### Puedo usarlo sin AutoJs6?

Sí. Las pantallas de herramientas, el editor de línea de tiempo y la grabadora de Studio funcionan por sí solos. AutoJs6 solo hace falta cuando los scripts procesan medios.

#### Por qué el complemento pide acceso a todos los archivos?

Los scripts pasan rutas de archivo normales como `/sdcard/DCIM/...`, y el complemento las lee y escribe directamente, igual que 3-Maple Image. Sin el permiso, el procesamiento informa de `STORAGE_PERMISSION_DENIED` y la pantalla del complemento explica cómo concederlo.

#### La grabación de pantalla necesita el complemento?

No. `video.record` y `audio.record` están integrados en AutoJs6 (compilación interna 6641 o posterior); el complemento añade los métodos de procesamiento y su propia grabadora de Studio.

******

### Permisos Y Seguridad

******

El complemento reduce al máximo sus permisos:

- Entradas protegidas: el servicio de capacidad y la entrada de activación exigen el permiso de complemento de AutoJs6 (`org.autojs.permission.PLUGIN`), así que otras aplicaciones no pueden llamarlos directamente.
- Almacenamiento: el acceso a todos los archivos solo se usa para las rutas que eligen los scripts y las pantallas de Studio; las salidas sin ruta van a las carpetas del complemento dentro de `Movies` y `Music`.
- Grabación: el micrófono y el audio interno del dispositivo solo se capturan mientras la grabadora de Studio está en marcha, con una notificación en primer plano; capturar el audio interno también requiere el consentimiento de captura de pantalla del sistema.
- Red: la versión 1.0.0 solo busca actualizaciones; desde la 1.1.0, la biblioteca FFmpeg se descarga cuando usted la pide y se comprueba su SHA-256 antes de cargarla.

******

### Biblioteca FFmpeg

******

Desde la versión 1.1.0, el complemento puede descargar una biblioteca FFmpeg opcional compilada bajo LGPL-2.1-or-later. La biblioteca no forma parte del APK: se descarga solo para su ABI cuando usted la pide, se carga de forma dinámica e incluye sus propios avisos de licencia y la ubicación del código fuente correspondiente. Sin ella, todas las funciones de la 1.0.0 siguen funcionando.

> Algunos códecs que admite FFmpeg, como H.265 / HEVC y AAC, pueden estar protegidos por patentes en algunos países o regiones. La biblioteca se ofrece solo por comodidad y no incluye ninguna licencia de patente; compruebe las normas que le correspondan antes de usarla.

******

### Interfaz Del Plugin

******

La siguiente información está dirigida a los desarrolladores del host AutoJs6 y de plugins; el host usa estos identificadores para descubrir el plugin y negociar capacidades:

```text
application id: io.github.supermonster003.autojs6.plugin.three.slate.studio
plugin id: three-slate-studio
engine: media-studio
variant: default
info service: org.autojs.plugin.INFO (category three-slate-studio)
capability service: org.autojs.plugin.MEDIA_STUDIO (category media-studio)
wake action: org.autojs.plugin.action.WAKE
minimum host build: 6641
native libraries: libandroidx.graphics.path.so of Jetpack Compose only (arm64-v8a, armeabi-v7a, x86_64, x86; 16 KB aligned); a single APK for every ABI
```

******

### Hoja De Ruta

******

Los planes, las decisiones de diseño y el progreso se registran como lista de verificación en ROADMAP.md. Los elementos sin marcar son planes, no funciones de la versión actual.

- [Ver ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### Historial De Versiones

******

#### v1.0.0

_2026/11/05_

- `Aviso` Versión preliminar de desarrollo: el centro de complementos de AutoJs6 puede detectar el complemento; las funciones de procesamiento, las pantallas de Studio y los métodos de procesamiento de los módulos de script video y audio aún no están disponibles
- `Aviso` Requiere AutoJs6 6.8.1 (6641) o posterior (la compilación mínima exacta se indicará cuando lleguen los cambios del host)
- `Función` Esqueleto del complemento: protocolo de activación Wake, servicio de identidad del centro de complementos y servicio de capacidad de procesamiento multimedia (versión de contrato 1)
- `Función` Pantalla de inicio con la concesión del acceso a todos los archivos
- `Función` Un único APK para todas las ABI: la única biblioteca nativa procede de Jetpack Compose y está alineada a páginas de memoria de 16 KB
- `Función` README, instrucciones del centro de complementos y registros de cambios en 10 idiomas, generados a partir de fuentes JSON
- `Dependencia` Se añade Jetpack Compose BOM 2026.09.00 (pantallas del complemento)
- `Dependencia` Se añade Navigation Compose 2.10.2
- `Dependencia` Se añade Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer y UI para el procesamiento y las vistas previas)
- `Dependencia` Se añade AndroidX Core KTX 1.18.0

##### Para más historial de versiones

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-es.md)

******

### Compilación

******

Esta sección está dirigida a desarrolladores que quieran compilar el plugin desde el código fuente.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### Licencia

******

El código del proyecto se distribuye bajo la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). Los componentes y datos de terceros se enumeran en los avisos de terceros.

- [Avisos de terceros (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### Enlaces

******

- Proyecto AutoJs6: https://github.com/SuperMonster003/AutoJs6
