<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Обработка видео и аудио, многодорожечный монтаж на таймлайне, переходы и субтитры, генерация кода скриптов из действий в интерфейсе</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Языки (Languages)

******

Текущий README.md поддерживает следующие языки:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- Русский [ru] # текущий
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### Введение

******

3-Slate Studio - плагин видео и аудио для AutoJs6 и одновременно приложение для монтажа, которое работает без AutoJs6. Он обрезает, склеивает, ускоряет, масштабирует, поворачивает, кадрирует видео и накладывает водяные знаки, извлекает кадры, создаёт GIF и перекодирует; обрезает, склеивает, микширует, плавно усиливает и ослабляет и конвертирует аудио; а его многодорожечный редактор таймлайна объединяет клипы с переходами, наложениями и субтитрами.

Обработка выполняется в плагине с помощью Jetpack Media3 Transformer и аппаратных кодеков устройства, поэтому хост AutoJs6 остаётся лёгким. Скрипты AutoJs6 обрабатывают медиа через глобальные модули `video` и `audio`; запись экрана (`video.record`) и запись звука (`audio.record`) встроены в AutoJs6 и работают без плагина. Каждый экран инструмента в плагине показывает эквивалентный код скрипта, который можно скопировать или открыть как новый скрипт в AutoJs6.

> Это предварительная версия для разработки: центр плагинов AutoJs6 может обнаружить плагин, но функции обработки, экраны Studio и методы обработки модулей `video` и `audio` ещё не готовы. Ниже описаны цели каждой версии; ход работы смотрите в плане развития.

******

### Основные Возможности

******

- Монтаж видео: обрезка по времени, склейка, изменение скорости (от 0.25x до 8x), масштабирование, поворот, кадрирование и отражение, текстовые или графические водяные знаки, извлечение или удаление звуковой дорожки и перекодирование (по умолчанию H.264, H.265 там, где устройство его поддерживает).
- Кадры и GIF: отдельные кадры, последовательности кадров и миниатюры в виде файлов изображений или объектов `Image`, а также GIF-анимации встроенного кодировщика.
- Аудио: обрезка, склейка, микширование нескольких входов, регулировка громкости с плавным нарастанием и затуханием, изменение скорости и конвертация в M4A, WAV, FLAC или OGG.
- Многодорожечный таймлайн: дорожки видео, аудио и наложений, 7 переходов (склейка, затемнение, растворение, сдвиг, шторка, масштаб, размытие) и субтитры с импортом и экспортом SRT и базовым подмножеством ASS, встраиваемые в изображение при экспорте.
- Генерация кода: каждый экран инструмента и экспорт таймлайна создают эквивалентный код `video.*` / `audio.*`, который можно скопировать или открыть как новый скрипт в AutoJs6.
- Рекордер Studio: запись микрофона, внутреннего звука устройства (Android 10 и новее) или обоих источников с экранов плагина.
- Библиотека FFmpeg (1.1.0): необязательная загрузка для вывода в MP3, WebM и MKV, прямой передачи команд через `video.ffmpeg` и полного рендеринга ASS.

******

### Использование

******

1. Скачайте APK со страницы [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) и установите его. Для всех ABI есть один общий пакет.
2. Откройте плагин и по запросу предоставьте доступ ко всем файлам, чтобы он мог читать и записывать выбранные вами пути.
3. Чтобы использовать методы обработки `video` и `audio` в скриптах, обновите AutoJs6 до внутренней сборки 6641 (6.8.1) или новее и включите плагин в центре плагинов.

> Плагин поддерживает устройства с Android 7.0 (API 24) и новее. Его единственная нативная библиотека входит в Jetpack Compose и выровнена по страницам памяти 16 KB, поэтому один пакет подходит для всех ABI.

******

### API Скриптов

******

Скрипты используют эти возможности через глобальные модули `video` и `audio` (также доступны как `$video` и `$audio`) (черновик проекта версии 1.0.0; основной считается документация выпущенной версии):

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

- У каждого метода обработки есть блокирующая форма и форма `xxxAsync`, возвращающая Promise; `options.onProgress(progress)` сообщает `percent`, `stage` и `etaMillis`, а остановка скрипта отменяет его задачи.
- Если `output` не указан, видео сохраняется в `Movies/3-Slate Studio/`, а аудио - в `Music/3-Slate Studio/`, с именем из исходного файла, операции и времени; результаты содержат фактический путь и сведения о медиа и регистрируются в медиатеке.
- `video.compose(timeline)` рендерит многодорожечный таймлайн, описанный объектом (дорожки, клипы, переходы, наложения, субтитры), в том же формате, который экспортирует редактор таймлайна.
- `video.tasks()` / `audio.tasks()` показывают и отменяют выполняемые задачи скрипта; при ошибке выбрасывается `MediaStudioError` со стабильным `code`, например `INVALID_OPTIONS` или `STORAGE_PERMISSION_DENIED`.

******

### Частые Вопросы

******

#### Можно ли пользоваться им без AutoJs6?

Да. Экраны инструментов, редактор таймлайна и рекордер Studio работают сами по себе. AutoJs6 нужен только тогда, когда медиа обрабатывают скрипты.

#### Почему плагин запрашивает доступ ко всем файлам?

Скрипты передают обычные пути к файлам, например `/sdcard/DCIM/...`, и плагин читает и записывает их напрямую, как 3-Maple Image. Без разрешения обработка возвращает `STORAGE_PERMISSION_DENIED`, а экран плагина объясняет, как его предоставить.

#### Нужен ли плагин для записи экрана?

Нет. `video.record` и `audio.record` встроены в AutoJs6 (внутренняя сборка 6641 или новее); плагин добавляет методы обработки и собственный рекордер Studio.

******

### Разрешения И Безопасность

******

Плагин запрашивает минимально необходимые разрешения:

- Защищённые точки входа: служба возможностей и точка активации требуют разрешения плагинов AutoJs6 (`org.autojs.permission.PLUGIN`), поэтому другие приложения не могут вызвать их напрямую.
- Хранилище: доступ ко всем файлам используется только для путей, выбранных скриптами и экранами Studio; результаты без указанного пути попадают в папки плагина внутри `Movies` и `Music`.
- Запись: микрофон и внутренний звук устройства захватываются только во время работы рекордера Studio, с уведомлением переднего плана; для захвата внутреннего звука также нужно системное согласие на захват экрана.
- Сеть: версия 1.0.0 только проверяет обновления; начиная с 1.1.0 библиотека FFmpeg загружается по вашему запросу, а перед загрузкой в память проверяется её SHA-256.

******

### Библиотека FFmpeg

******

Начиная с версии 1.1.0 плагин может загрузить необязательную библиотеку FFmpeg, собранную по лицензии LGPL-2.1-or-later. Библиотека не входит в APK: она загружается только для ABI вашего устройства и только по вашему запросу, подключается динамически и сопровождается собственными уведомлениями о лицензии и сведениями о том, где получить соответствующий исходный код. Без неё все возможности версии 1.0.0 продолжают работать.

> Некоторые кодеки, поддерживаемые FFmpeg, например H.265 / HEVC и AAC, в ряде стран и регионов могут быть защищены патентами. Библиотека предоставляется только для удобства и не включает патентной лицензии; перед использованием проверьте применимые к вам правила.

******

### Интерфейс Плагина

******

Следующая информация предназначена разработчикам хоста AutoJs6 и плагинов; хост использует эти идентификаторы для обнаружения плагина и согласования возможностей:

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

### Дорожная Карта

******

Планы, проектные решения и ход работ ведутся в виде списка с отметками в ROADMAP.md. Неотмеченные пункты являются планами, а не возможностями текущей версии.

- [Открыть ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### История Выпусков

******

#### v1.0.0

_2026/11/05_

- `Подсказка` Предварительная версия для разработки: центр плагинов AutoJs6 может обнаружить плагин; функции обработки, экраны Studio и методы обработки модулей скриптов video и audio ещё не готовы
- `Подсказка` Требуется AutoJs6 6.8.1 (6641) или новее (точная минимальная сборка будет указана после внесения изменений в хост)
- `Функция` Каркас плагина: протокол активации Wake, служба идентификации центра плагинов и служба возможностей обработки медиа (версия контракта 2)
- `Функция` Главный экран с предоставлением доступа ко всем файлам
- `Функция` Один APK для всех ABI: единственная нативная библиотека входит в Jetpack Compose и выровнена по страницам памяти 16 KB
- `Функция` README, инструкции центра плагинов и журналы изменений на 10 языках, созданные из исходных файлов JSON
- `Зависимость` Добавлен Jetpack Compose BOM 2026.09.00 (экраны плагина)
- `Зависимость` Добавлен Navigation Compose 2.10.2
- `Зависимость` Добавлен Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer и UI для обработки и предпросмотра)
- `Зависимость` Добавлен AndroidX Core KTX 1.18.0

##### Больше истории выпусков

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-ru.md)

******

### Сборка

******

Этот раздел предназначен разработчикам, желающим собрать плагин из исходников.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### Лицензия

******

Код проекта распространяется по лицензии [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). Сторонние компоненты и данные перечислены в уведомлениях о сторонних компонентах.

- [Сведения о сторонних компонентах (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### Ссылки

******

- Проект AutoJs6: https://github.com/SuperMonster003/AutoJs6
