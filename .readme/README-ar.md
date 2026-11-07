<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>معالجة الفيديو والصوت, تحرير متعدد المسارات على خط زمني, انتقالات وترجمات, مع توليد شيفرة السكربت من شاشات التطبيق</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### اللغات (Languages)

******

يدعم README.md الحالي اللغات التالية:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- العربية [ar] # الحالية

******

### مقدمة

******

⁨3-Slate Studio⁩ هي إضافة الفيديو والصوت في AutoJs6, وهي أيضا تطبيق تحرير يعمل دون AutoJs6. تقص مقاطع الفيديو وتدمجها وتغير سرعتها وحجمها وتدورها وتقتطعها وتضيف إليها علامات مائية, وتستخرج الإطارات وتنشئ صور GIF وتعيد الترميز; وتقص الصوت وتدمجه وتمزجه وتضيف إليه التلاشي وتحوله; ويجمع محرر الخط الزمني متعدد المسارات المقاطع مع الانتقالات والتراكبات والترجمات.

تجري المعالجة داخل الإضافة باستخدام Jetpack Media3 Transformer وبرامج الترميز العتادية في الجهاز, لذا يبقى مضيف AutoJs6 خفيفا. تعالج سكربتات AutoJs6 الوسائط عبر الوحدتين العامتين `video` و `audio`; أما تسجيل الشاشة (`video.record`) وتسجيل الصوت (`audio.record`) فمدمجان في AutoJs6 ويعملان دون الإضافة. تعرض كل شاشة أداة في الإضافة شيفرة السكربت المكافئة, ويمكن نسخها أو فتحها سكربتا جديدا في AutoJs6.

> الإصدار 1.0.0 هو أول إصدار رسمي. تتطلب معالجة الوسائط في السكربتات AutoJs6 6.8.1 (الإصدار الداخلي 6641) أو أحدث; ولا تعتمد شاشات الإضافة ومحرر الخط الزمني ومسجل Studio على AutoJs6.

******

### أبرز الميزات

******

- تحرير الفيديو: القص حسب الوقت والدمج وتغيير السرعة (من 0.25x إلى 8x) وتغيير الحجم والتدوير والاقتطاع والقلب وعلامات مائية نصية أو صورية واستخراج المسار الصوتي أو إزالته وإعادة الترميز (H.264 افتراضيا و H.265 إذا كان في الجهاز مرمّز H.265).
- الإطارات و GIF: إطارات مفردة وتسلسلات إطارات وصور مصغرة كملفات PNG أو JPG أو كائنات `Image`, وصور GIF متحركة من المرمّز المدمج.
- الصوت: القص والدمج ومزج عدة مدخلات (يُخفَّض المستوى تلقائيا إذا كان المجموع سيتشوه) وضبط مستوى الصوت مع التلاشي عند البداية والنهاية وتغيير السرعة والتحويل إلى M4A أو WAV أو FLAC أو OGG (يتطلب OGG نظام Android 10 أو أحدث).
- محرر خط زمني متعدد المسارات: مسارات للفيديو والصوت والتراكبات والترجمات, و 7 انتقالات (قطع, تلاش, ذوبان, انزلاق, مسح, تكبير, تمويه), ومعاينة مباشرة, وتراجع وإعادة, ومشاريع تُحفظ بصيغة `.slate.json`; وتدعم الترجمات استيراد SRT وتصديرها ومجموعة أساسية من ASS, وتُدمج في الصورة عند التصدير.
- صندوق الأدوات: معالج لكل ميزة معالجة, ونماذج تعرض القيم الافتراضية والنطاقات; وتعمل المهام في قائمة انتظار تعرض التقدم وسبب الفشل, وتحتفظ المهام الطويلة بإشعار في المقدمة.
- توليد الشيفرة: ينتج كل معالج وتصدير الخط الزمني والمسجل سكربت `video.*` / `audio.*` المكافئ, جاهزا للنسخ أو للفتح سكربتا جديدا في AutoJs6.
- مسجل Studio: يسجل الميكروفون أو صوت الجهاز الداخلي (Android 10 أو أحدث) أو كليهما من شاشات الإضافة بصيغة M4A أو WAV, مع الإيقاف المؤقت ومتابعة التسجيل والشاشة مطفأة.
- مكتبة FFmpeg (مخطط لها في 1.1.0): تنزيل اختياري يضيف صيغا مثل MP3 و WebM و MKV وعرض ترجمات ASS بالكامل.

******

### لقطات الشاشة

******

التُقطت هذه الصور على هواتف تعمل بنظام Android 12 و 13, بالسمة الفاتحة وبواجهة باللغة الصينية المبسطة.

| مكتبة الوسائط | صندوق الأدوات | محرر الخط الزمني | مسجل Studio |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/library.png?raw=true" alt="library" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/toolbox.png?raw=true" alt="toolbox" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/timeline.png?raw=true" alt="timeline" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/recorder.png?raw=true" alt="recorder" width="200" /> |

******

### الاستخدام

******

1. ثبّت ⁨3-Slate Studio⁩ من مركز الإضافات في AutoJs6, أو نزّل ملف APK من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) وثبّته. توجد حزمة واحدة لجميع واجهات ABI.
2. افتح الإضافة وامنحها الوصول إلى جميع الملفات عند الطلب, حتى تتمكن من قراءة المسارات التي تختارها والكتابة فيها.
3. لاستخدام طرق المعالجة في `video` و `audio` داخل السكربتات, استخدم AutoJs6 6.8.1 (الإصدار الداخلي 6641) أو أحدث وفعّل الإضافة في مركز الإضافات.

> تدعم الإضافة الأجهزة التي تعمل بنظام Android 7.0 (API 24) أو أحدث. مكتبتها الأصلية الوحيدة تأتي من Jetpack Compose وهي محاذاة على صفحات ذاكرة بحجم 16 KB, لذا تخدم حزمة واحدة جميع واجهات ABI.

******

### واجهة برمجة النصوص

******

تستخدم السكربتات هذه الميزات عبر الوحدتين العامتين `video` و `audio` (ويمكن كتابتهما أيضا `$video` و `$audio`) (يصف توثيق AutoJs6 الخاص بـ [video](https://docs.autojs6.com/#/video) و [audio](https://docs.autojs6.com/#/audio) كل طريقة وخيار):

```javascript
let a = video.trim("/sdcard/Movies/a.mp4", { start: 0, end: 5000 });
let b = video.overlay("/sdcard/Movies/b.mp4", [{ type: "text", text: "AutoJs6", x: "50%", y: "8%", fontSize: 48 }]);
let joined = video.concat([a.path, b.path], { onProgress: (p) => console.log(p.stage, p.percent) });
toastLog(video.toGif(joined.path, { fps: 10, width: 480 }).path);

audio.mix([{ src: "/sdcard/Music/voice.m4a" }, { src: "/sdcard/Music/bgm.m4a", volume: 0.3 }]);

images.requestScreenCapture();
let recording = video.record({ audio: "none", frameRate: 30 });
sleep(10000);
toastLog(recording.stop().path);

video.compose({
    video: [{ clips: [
        { src: "/sdcard/Movies/a.mp4", end: 8000 },
        { src: "/sdcard/Movies/b.mp4", start: 2000, end: 10000, transition: { type: "dissolve", duration: 500 } },
    ] }],
    audio: [{ clips: [{ src: "/sdcard/Music/bgm.m4a", end: 15500, volume: 0.4, fadeIn: 1000, fadeOut: 2000 }] }],
    overlays: [{ type: "text", text: "AutoJs6", from: 1000, to: 4000, x: "50%", y: "10%", fontSize: 48 }],
});

video.transcodeAsync("/sdcard/Movies/big.mp4", { codec: "h265", height: 720 }).then((r) => toast(r.path));
```

- لكل طريقة معالجة صيغة متزامنة تنتظر حتى الانتهاء وصيغة `xxxAsync` تعيد Promise; تبلغ `options.onProgress(progress)` عن `percent` و `stage` و `etaMillis`, وإيقاف السكربت يلغي مهامه.
- عند حذف `output` تُحفظ مقاطع الفيديو في `Movies/3-Slate Studio/` والصوت في `Music/3-Slate Studio/` والإطارات والصور المصغرة و GIF في `Pictures/3-Slate Studio/`, باسم يتكون من المصدر والعملية والوقت; وتحمل النتائج المسار الفعلي ومعلومات الوسائط وتُسجَّل في مكتبة الوسائط.
- تعرض `video.compose(timeline)` خطا زمنيا متعدد المسارات موصوفا ككائن (حتى 8 مسارات فيديو و 16 مسار صوت, ومقاطع, وانتقالات, وتراكبات, وترجمات), بالصيغة نفسها التي يصدّرها محرر الخط الزمني.
- يسجّل AutoJs6 نفسه `video.record` و `audio.record`, وعند حذف المسار تُحفظ الملفات في `Movies/AutoJs6/` و `Music/AutoJs6/`; ويتطلب تسجيل الشاشة استدعاء `images.requestScreenCapture` أولا.
- تسرد `video.tasks()` / `audio.tasks()` المهام الجارية للسكربت وتلغيها; وعند الفشل تُرمى `MediaStudioError` برمز `code` ثابت مثل `INVALID_OPTIONS` أو `STORAGE_PERMISSION_DENIED`.

******

### سكربتات الأمثلة

******

يحتوي مجلد [samples](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/tree/master/samples) في هذا المستودع على 4 سكربتات جاهزة للتشغيل, وتتضمن الأمثلة المرفقة مع AutoJs6 السكربتات نفسها (مجلد ⁨3-Slate Studio⁩):

- تسجيل الشاشة 10 ثوان وتحويله إلى GIF: يسجل `video.record` الشاشة دون صوت, ثم يحوّله `video.toGif` إلى GIF بمعدل 10 إطارات في الثانية وعرض 480 بكسل (يطلب التشغيل الأول تأكيد التقاط الشاشة).
- تسجيل الصوت وتحويله إلى FLAC: يسجل `audio.record` الميكروفون, ثم يحوّله `audio.convert` إلى FLAC دون فقدان (يطلب التشغيل الأول إذن التسجيل).
- القص والعلامة المائية والدمج: يقرأ `video.probe` معلومات المصدر, ويحتفظ `video.trim` بأول 5 ثوان, ويضيف `video.overlay` علامة مائية نصية, ويدمج `video.concat` الجزأين ويبلغ عن التقدم.
- تركيب الخط الزمني: يجمع `video.compose` مقطعي فيديو (مع ذوبان) وموسيقى خلفية (مع تلاش) وتراكبا نصيا في فيديو عمودي واحد.

يقرأ المثالان الأخيران افتراضيا `/sdcard/Movies/a.mp4` و `/sdcard/Movies/b.mp4` و `/sdcard/Music/bgm.m4a`; ضع وسائطك هناك أو غيّر المسارات.

******

### التوافق

******

تدعم الإضافة Android 7.0 (API 24) أو أحدث, وتعمل دون AutoJs6. ويتطلب تفعيلها في مركز الإضافات واستخدام طرق المعالجة في `video` و `audio` داخل السكربتات AutoJs6 6.8.1 (الإصدار الداخلي 6641) أو أحدث (F10); وتعمل الوحدتان في محركات Rhino و Node.js و Bun.

تشير الأرقام بين القوسين إلى أدلة [الملحق F من خارطة الطريق](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md). وتعني "لم يُختبر" أن الإصدار أو الصيغة لم يُتحقق منه قبل صدور 1.0.0, لا أنه لا يعمل.

**حسب إصدار Android**

| Android | الأجهزة | المعالجة | شاشات الإضافة | مسجل Studio | سكربتات الأمثلة |
| --- | --- | --- | --- | --- | --- |
| 7.0 (API 24) | محاكي Android | نجح (F5) | نجح (F4, F6) | لم يُختبر | لم يُختبر |
| 7.1 - 8.1 (API 25 - 27) | - | لم يُختبر | لم يُختبر | لم يُختبر | لم يُختبر |
| 9 (API 28) | Sony G8441 | نجح (F3) | نجحت شاشة التسجيل (F8) | نجح, الميكروفون فقط (F8) | لم يُختبر |
| 10 - 11 (API 29 - 30) | - | لم يُختبر | لم يُختبر | لم يُختبر | لم يُختبر |
| 12 (API 31) | Sony XQ-AT72 | نجح (F3, F5) | نجح (F4, F5, F7) | نجح, مع صوت الجهاز الداخلي (F8) | لم يُختبر |
| 12L (API 32) | - | لم يُختبر | لم يُختبر | لم يُختبر | لم يُختبر |
| 13 (API 33) | Sony XQ-DQ72, Redmi 22120RN86C | نجح (F1, F3) | نجح (F6, F8) | نجح, مع صوت الجهاز الداخلي (F8) | نجح (F9) |
| 14 (API 34) | - | لم يُختبر | لم يُختبر | لم يُختبر | لم يُختبر |
| 15 (API 35) | Xiaomi Pad 23046RP50C, OnePlus OPD2413, محاكي Android | نجح (F1, F2, F3, F5) | نجح (F4, F5, F6) | نجح, مع صوت الجهاز الداخلي (F8) | لم يُختبر |

المعالجة: عمليات السكربت الـ 25 أو معالجات صندوق الأدوات الـ 20; شاشات الإضافة: مكتبة الوسائط وصندوق الأدوات والمهام والإعدادات ومحرر الخط الزمني; سكربتات الأمثلة: الأمثلة الأربعة أعلاه.

**حسب الصيغة**

| الصيغة | كمدخل | كمخرج | ملاحظات |
| --- | --- | --- | --- |
| MP4: فيديو H.264 + صوت AAC | نجح (F2, F3) | نجح (F2, F3) | صيغة الفيديو الافتراضية |
| MP4: فيديو H.265 / HEVC | لم يُختبر | نجح (F1, F2, F3) | فقط إذا كان في الجهاز مرمّز H.265; وهو موجود في الهواتف والأجهزة اللوحية الستة المختبرة |
| M4A (AAC) | نجح (F2, F3) | نجح (F2, F3, F8) | صيغة الصوت الافتراضية والصيغة الافتراضية لمسجل Studio |
| WAV (PCM بدقة 16 بت) | نجح (F2) | نجح (F2, F3) | دون فقدان, ملفات كبيرة |
| FLAC | لم يُختبر | نجح (F2, F3) | ضغط دون فقدان |
| OGG (Opus) | لم يُختبر | نجح (F2, F3) | يتطلب Android 10 أو أحدث (يكتب النظام OGG بدءا من Android 10); اختُبر على Android 12 - 15 |
| GIF | لم يُختبر | نجح (F2, F3) | مرمّز مدمج, من 1 إلى 50 إطارا في الثانية |
| صور PNG و JPG | نجح (F2, F3) | نجح (F2, F3) | المدخل: صور التراكبات; المخرج: الإطارات والصور المصغرة |
| ترجمات SRT | نجح (F2, F3, F6) | نجح (F2) | تُستورد إلى محرر الخط الزمني أو تُدمج في الصورة |
| ترجمات ASS (مجموعة أساسية) | نجح (F2) | نجح (F2, F3) | تُتجاهل الوسوم غير المدعومة مع تحذير; والعرض الكامل لـ ASS مخطط له في 1.1.0 |
| MP3 و AMR وصيغ صوت أخرى | لم يُختبر | غير متاح في 1.0.0 | تعتمد القراءة على مفككات الترميز في نظام الجهاز; وإخراج MP3 مخطط له مع مكتبة FFmpeg في 1.1.0 |
| WebM و MKV | لم يُختبر | غير متاح في 1.0.0 | مخطط له مع مكتبة FFmpeg في 1.1.0 |

******

### الأسئلة الشائعة

******

#### هل يمكنني استخدامها دون AutoJs6?

نعم. تعمل مكتبة الوسائط وصندوق الأدوات ومحرر الخط الزمني ومسجل Studio وحدها. لا يلزم AutoJs6 إلا عندما تعالج السكربتات الوسائط.

#### لماذا تطلب الإضافة الوصول إلى جميع الملفات?

تمرر السكربتات مسارات ملفات عادية مثل `/sdcard/DCIM/...`, وتقرأها الإضافة وتكتب فيها مباشرة كما تفعل ⁨3-Maple Image⁩. دون هذا الإذن تبلغ المعالجة عن `STORAGE_PERMISSION_DENIED`, وتشرح شاشة الإضافة كيفية منحه. تعيد `video.frame` كائن `Image`, لكن الصورة تنتقل إلى AutoJs6 عبر ملف مؤقت في `Movies/3-Slate Studio/`, لذا تحتاج إلى الإذن نفسه.

#### هل يحتاج تسجيل الشاشة إلى الإضافة?

لا. `video.record` و `audio.record` مدمجتان في AutoJs6 (6.8.1 أو أحدث); وتضيف الإضافة طرق المعالجة ومسجل Studio الخاص بها.

#### هل يمكنني استخدامها مع إصدار أقدم من AutoJs6?

تحتاج السكربتات إلى AutoJs6 6.8.1 (الإصدار الداخلي 6641) أو أحدث لاستخدام الإضافة; فالإصدارات الأقدم لا تحتوي بعد على الوحدتين `video` و `audio`. وتبقى الإضافة نفسها قابلة للاستخدام كتطبيق تحرير مستقل.

#### يبلغ تصدير الخط الزمني عن `CODEC_ERROR` ويطلب مسارات أقل. ماذا أفعل?

أثناء الانتقال المتداخل (ذوبان, انزلاق, مسح, تكبير, تمويه) يفك مسار الفيديو ترميز مقطعين في وقت واحد, وعدد برامج الترميز في الجهاز محدود. صدّرت هواتف Sony الثلاثة المختبرة 6 مسارات فيديو بانتقالات مع 16 مسار صوت, بينما انتهى 8 + 16 بـ `CODEC_ERROR`; استخدم مسارات فيديو أقل أو انتقالات متداخلة أقل.

******

### الأذونات والأمان

******

الأذونات التي تطلبها الإضافة وغرضها:

- الوصول إلى جميع الملفات (`MANAGE_EXTERNAL_STORAGE`; وأذونات التخزين في Android 10 والإصدارات الأقدم): قراءة المسارات التي تختارها السكربتات والشاشات والكتابة فيها, وكتابة المخرجات التي لا مسار لها في مجلدات `3-Slate Studio` داخل `Movies` و `Music` و `Pictures`.
- الوصول إلى الوسائط (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, Android 13 أو أحدث): عرض مقاطع الفيديو والصوت في الجهاز ضمن مكتبة الوسائط في الشاشة الرئيسية.
- الميكروفون (`RECORD_AUDIO`): لا يستخدمه إلا مسجل Studio; أما `audio.record` و `video.record` في السكربتات فيسجلهما AutoJs6 بإذن التسجيل الخاص به.
- التقاط الشاشة (موافقة النظام على مشاركة الشاشة, وليس إذنا في البيان): يلزم عندما يلتقط مسجل Studio صوت الجهاز الداخلي (الصوت الداخلي أو المختلط); ويطلبه النظام عند بدء التسجيل.
- الإشعارات (`POST_NOTIFICATIONS`, Android 13 أو أحدث): عرض تقدم المعالجة وإشعار التسجيل مع زري الإيقاف المؤقت والإيقاف; ودون هذا الإذن لا تظهر هذه الإشعارات.
- خدمات المقدمة وقفل التنبيه (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`): متابعة المعالجة الطويلة في الخلفية والتسجيل والشاشة مطفأة.
- الشبكة (`INTERNET`, `ACCESS_NETWORK_STATE`): في 1.0.0 للتحقق من التحديثات فقط, إذ يُقرأ فهرس الإضافات الرسمي مرة كل 12 ساعة على الأكثر عند فتح الشاشة الرئيسية, ويمكن إيقاف ذلك من الإعدادات.
- إذن إضافات AutoJs6 (`org.autojs.permission.PLUGIN`): التواصل مع AutoJs6.

تبقي الإضافة أذوناتها في أضيق نطاق ممكن:

- نقاط دخول محمية: تتطلب خدمة القدرات ونقطة التفعيل إذن إضافات AutoJs6 (`org.autojs.permission.PLUGIN`), فلا يمكن للتطبيقات الأخرى استدعاؤها مباشرة.
- التخزين: لا يُستخدم الوصول إلى جميع الملفات إلا للمسارات التي تختارها السكربتات وشاشات Studio; وتذهب المخرجات التي لا مسار لها إلى مجلدات `3-Slate Studio` داخل `Movies` و `Music` و `Pictures` فقط.
- التسجيل: لا يُلتقط الميكروفون وصوت الجهاز الداخلي إلا أثناء عمل مسجل Studio, مع إشعار في المقدمة; ويتطلب التقاط صوت الجهاز الداخلي أيضا موافقة النظام على التقاط الشاشة.
- الشبكة: يقتصر الإصدار 1.0.0 على التحقق من التحديثات; أما مكتبة FFmpeg المخطط لها (1.1.0) فلن تُنزَّل إلا عند طلبك, وسيُتحقق من SHA-256 الخاص بها قبل تحميلها.

******

### مكتبة FFmpeg

******

مكتبة FFmpeg مخطط لها في الإصدار 1.1.0 وستُبنى بموجب LGPL-2.1-or-later. لن تكون جزءا من ملف APK: ستُنزَّل لواجهة ABI في جهازك فقط عند طلبك, وتُحمَّل ديناميكيا, وترافقها إشعارات الترخيص الخاصة بها وموقع الحصول على الشيفرة المصدرية المقابلة. ومن دونها تبقى جميع ميزات الإصدار 1.0.0 متاحة. وستُشرح صيغ المكتبة وواجهة السكربتات عند صدور 1.1.0.

> قد تكون بعض برامج الترميز التي يدعمها FFmpeg, مثل H.265 / HEVC و AAC, محمية ببراءات اختراع في بعض البلدان أو المناطق. تُقدَّم المكتبة للتيسير فقط ولا تتضمن أي ترخيص لبراءات الاختراع; تحقق من القواعد التي تنطبق عليك قبل استخدامها.

******

### واجهة المكون الإضافي

******

المعلومات التالية موجهة لمطوري مضيف AutoJs6 والمكونات الإضافية; يستخدم المضيف هذه المعرفات لاكتشاف المكون والتفاوض على القدرات:

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

### خارطة الطريق

******

تتابع الخطط وقرارات التصميم والتقدم في شكل قائمة تحقق في ROADMAP.md. العناصر غير المحددة خطط وليست ميزات في الإصدار الحالي.

- [عرض ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### سجل الإصدارات

******

#### v1.0.0

_2026/11/08_

- `تلميح` تتطلب معالجة الوسائط عبر video / audio في السكربتات AutoJs6 6.8.1 (الإصدار الداخلي 6641) أو أحدث; ولا تعتمد شاشات الإضافة ومحرر الخط الزمني ومسجل Studio على AutoJs6
- `تلميح` واجهة السكربتات موصوفة في فصلي [video](https://docs.autojs6.com/#/video) و [audio](https://docs.autojs6.com/#/audio) من توثيق AutoJs6
- `ميزة` معالجة الفيديو: القص والدمج والسرعة (0.25x - 8x) وتغيير الحجم والتدوير والاقتطاع والقلب وعلامات مائية نصية أو صورية واستخراج المسار الصوتي أو إزالته وإعادة الترميز (H.264, أو H.265 إذا دعمه الجهاز)
- `ميزة` الإطارات والصور المصغرة و GIF: ملفات PNG / JPG أو كائنات Image, ومرمّز GIF مدمج (لوحة ألوان, تنقيط, تكرار)
- `ميزة` معالجة الصوت: القص والدمج والمزج (يُخفَّض تلقائيا إذا كان سيتشوه) ومستوى الصوت والتلاشي والسرعة والتحويل إلى M4A / WAV / FLAC / OGG (يتطلب OGG نظام Android 10+)
- `ميزة` تركيب متعدد المسارات عبر video.compose: حتى 8 مسارات فيديو و 16 مسار صوت, و 7 انتقالات, وتراكبات نصية / صورية وترجمات
- `ميزة` الترجمات: قراءة SRT وكتابتها ومجموعة أساسية من ASS, ودمجها في الصورة عبر video.burnSubtitles, و video.subtitles.parse / write لملفات الترجمة
- `ميزة` وحدتا سكربتات AutoJs6 video / audio (AutoJs6 6.8.1, و Rhino و Node.js و Bun): لكل طريقة معالجة صيغة xxxAsync واستدعاء للتقدم, وإيقاف السكربت يلغي مهامه
- `ميزة` شاشات Studio: مكتبة الوسائط وصندوق الأدوات وقائمة المهام والإعدادات; يتبع المظهر AutoJs6 أو يستخدم لغته ووضعه الليلي ولون سمته الخاص, وترشدك الإعدادات إلى منح الوصول إلى جميع الملفات واستخدام البطارية دون قيود
- `ميزة` معالجات صندوق الأدوات: معالج لكل ميزة معالجة, ونماذج تعرض القيم الافتراضية والنطاقات, وصفحة نتيجة تعرض المخرج ومعلومات الوسائط والسكربت المكافئ
- `ميزة` محرر الخط الزمني: تحرير متعدد المسارات, وانتقالات, وتحرير التراكبات والترجمات, ومعاينة مباشرة, وتراجع / إعادة, ومشاريع تُحفظ بصيغة .slate.json, وتصدير إلى قائمة المهام
- `ميزة` توليد الشيفرة: تعرض المعالجات والخط الزمني والمسجل السكربت الكامل المكافئ, جاهزا للنسخ أو للفتح في AutoJs6 سكربتا جديدا
- `ميزة` مسجل Studio: الميكروفون أو صوت الجهاز الداخلي (Android 10+) أو كلاهما, بصيغة M4A / WAV, مع الإيقاف المؤقت / الاستئناف والتسجيل والشاشة مطفأة والتحكم من الإشعار
- `ميزة` تعمل المهام الطويلة في خدمة في المقدمة مع إشعار بالتقدم; وتختار الإعدادات تفضيل مرمّز العتاد وعدد المهام التي تعمل معا (1 - 2)
- `ميزة` تذهب المخرجات التي لا مسار لها إلى مجلدات ⁨3-Slate Studio⁩ داخل Movies و Music و Pictures وتُسجَّل في مكتبة الوسائط
- `ميزة` التحقق من التحديثات: يقرأ فهرس الإضافات الرسمي مرة كل 12 ساعة على الأكثر عند فتح الشاشة الرئيسية, ويمكن تجاهل إصدار معين
- `ميزة` ملف APK واحد لجميع واجهات ABI: المكتبة الأصلية الوحيدة تأتي من Jetpack Compose وهي محاذاة على صفحات ذاكرة بحجم 16 KB
- `ميزة` الشاشات و README وتعليمات مركز الإضافات وسجل التغييرات بـ 10 لغات
- `تبعية` إرفاق Jetpack Media3 1.10.1 (Transformer و Effect و Muxer و ExoPlayer و UI, للمعالجة والمعاينة)
- `تبعية` إرفاق Jetpack Compose BOM 2026.09.00 (شاشات الإضافة)
- `تبعية` إرفاق AndroidX Activity Compose 1.13.0
- `تبعية` إرفاق Navigation Compose 2.10.2
- `تبعية` إرفاق AndroidX Core KTX 1.18.0
- `تبعية` إرفاق Material Color Utilities (MaterialKolor) 4.1.1 (لون سمة شاشات الإضافة)

##### لمزيد من سجل الإصدارات

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-ar.md)

******

### البناء

******

هذا القسم موجه للمطورين الراغبين في بناء المكون من المصدر.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### الترخيص

******

شيفرة المشروع مرخصة بموجب [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). المكونات والبيانات من أطراف ثالثة مذكورة في إشعارات الأطراف الثالثة.

- [إشعارات الأطراف الثالثة (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### روابط

******

- مشروع AutoJs6: https://github.com/SuperMonster003/AutoJs6
