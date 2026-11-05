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

> هذا إصدار معاينة للتطوير: يستطيع مركز الإضافات في AutoJs6 اكتشاف الإضافة, لكن ميزات المعالجة وشاشات Studio وطرق المعالجة في الوحدتين `video` و `audio` لم تُسلَّم بعد. يصف ما يلي أهداف تصميم كل إصدار, وراجع خارطة الطريق لمعرفة التقدم.

******

### أبرز الميزات

******

- تحرير الفيديو: القص حسب الوقت والدمج وتغيير السرعة (من 0.25x إلى 8x) وتغيير الحجم والتدوير والاقتطاع والقلب وعلامات مائية نصية أو صورية واستخراج المسار الصوتي أو إزالته وإعادة الترميز (H.264 افتراضيا و H.265 حيث يدعمه الجهاز).
- الإطارات و GIF: إطارات مفردة وتسلسلات إطارات وصور مصغرة كملفات صور أو كائنات `Image`, وصور GIF متحركة من المرمّز المدمج.
- الصوت: القص والدمج ومزج عدة مدخلات وضبط مستوى الصوت مع التلاشي عند البداية والنهاية وتغيير السرعة والتحويل إلى M4A أو WAV أو FLAC أو OGG.
- خط زمني متعدد المسارات: مسارات للفيديو والصوت والتراكبات, و 7 انتقالات (قطع, تلاش, ذوبان, انزلاق, مسح, تكبير, تمويه), وترجمات مع استيراد SRT وتصديرها ومجموعة أساسية من ASS, تُدمج في الصورة عند التصدير.
- توليد الشيفرة: تنتج كل شاشة أداة وكل تصدير للخط الزمني شيفرة `video.*` / `audio.*` المكافئة, جاهزة للنسخ أو للفتح سكربتا جديدا في AutoJs6.
- مسجل Studio: يسجل الميكروفون أو صوت الجهاز الداخلي (Android 10 أو أحدث) أو كليهما من شاشات الإضافة.
- مكتبة FFmpeg (1.1.0): تنزيل اختياري لإخراج MP3 و WebM و MKV, وتمرير الأوامر مباشرة عبر `video.ffmpeg`, وعرض ASS الكامل.

******

### الاستخدام

******

1. نزّل ملف APK من [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) وثبّته. توجد حزمة واحدة لجميع واجهات ABI.
2. افتح الإضافة وامنحها الوصول إلى جميع الملفات عند الطلب, حتى تتمكن من قراءة المسارات التي تختارها والكتابة فيها.
3. لاستخدام طرق المعالجة في `video` و `audio` داخل السكربتات, حدّث AutoJs6 إلى الإصدار الداخلي 6641 (6.8.1) أو أحدث وفعّل الإضافة في مركز الإضافات.

> تدعم الإضافة الأجهزة التي تعمل بنظام Android 7.0 (API 24) أو أحدث. مكتبتها الأصلية الوحيدة تأتي من Jetpack Compose وهي محاذاة على صفحات ذاكرة بحجم 16 KB, لذا تخدم حزمة واحدة جميع واجهات ABI.

******

### واجهة برمجة النصوص

******

تستخدم السكربتات هذه الميزات عبر الوحدتين العامتين `video` و `audio` (ويمكن كتابتهما أيضا `$video` و `$audio`) (مسودة تصميم الإصدار 1.0.0, والمرجع هو توثيق الإصدار المنشور):

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

- لكل طريقة معالجة صيغة متزامنة تنتظر حتى الانتهاء وصيغة `xxxAsync` تعيد Promise; تبلغ `options.onProgress(progress)` عن `percent` و `stage` و `etaMillis`, وإيقاف السكربت يلغي مهامه.
- عند حذف `output` تُحفظ مقاطع الفيديو في `Movies/3-Slate Studio/` والصوت في `Music/3-Slate Studio/`, باسم يتكون من المصدر والعملية والوقت; وتحمل النتائج المسار الفعلي ومعلومات الوسائط وتُسجَّل في مكتبة الوسائط.
- تعرض `video.compose(timeline)` خطا زمنيا متعدد المسارات موصوفا ككائن (مسارات, مقاطع, انتقالات, تراكبات, ترجمات), بالصيغة نفسها التي يصدّرها محرر الخط الزمني.
- تسرد `video.tasks()` / `audio.tasks()` المهام الجارية للسكربت وتلغيها; وعند الفشل تُرمى `MediaStudioError` برمز `code` ثابت مثل `INVALID_OPTIONS` أو `STORAGE_PERMISSION_DENIED`.

******

### الأسئلة الشائعة

******

#### هل يمكنني استخدامها دون AutoJs6?

نعم. تعمل شاشات الأدوات ومحرر الخط الزمني ومسجل Studio وحدها. لا يلزم AutoJs6 إلا عندما تعالج السكربتات الوسائط.

#### لماذا تطلب الإضافة الوصول إلى جميع الملفات?

تمرر السكربتات مسارات ملفات عادية مثل `/sdcard/DCIM/...`, وتقرأها الإضافة وتكتب فيها مباشرة كما تفعل ⁨3-Maple Image⁩. دون هذا الإذن تبلغ المعالجة عن `STORAGE_PERMISSION_DENIED`, وتشرح شاشة الإضافة كيفية منحه.

#### هل يحتاج تسجيل الشاشة إلى الإضافة?

لا. `video.record` و `audio.record` مدمجتان في AutoJs6 (الإصدار الداخلي 6641 أو أحدث); وتضيف الإضافة طرق المعالجة ومسجل Studio الخاص بها.

******

### الأذونات والأمان

******

تبقي الإضافة أذوناتها في أضيق نطاق ممكن:

- نقاط دخول محمية: تتطلب خدمة القدرات ونقطة التفعيل إذن إضافات AutoJs6 (`org.autojs.permission.PLUGIN`), فلا يمكن للتطبيقات الأخرى استدعاؤها مباشرة.
- التخزين: لا يُستخدم الوصول إلى جميع الملفات إلا للمسارات التي تختارها السكربتات وشاشات Studio; وتذهب المخرجات التي لا مسار لها إلى مجلدات الإضافة داخل `Movies` و `Music`.
- التسجيل: لا يُلتقط الميكروفون وصوت الجهاز الداخلي إلا أثناء عمل مسجل Studio, مع إشعار في المقدمة; ويتطلب التقاط صوت الجهاز الداخلي أيضا موافقة النظام على التقاط الشاشة.
- الشبكة: يقتصر الإصدار 1.0.0 على التحقق من التحديثات; وبدءا من 1.1.0 تُنزَّل مكتبة FFmpeg عند طلبك ويُتحقق من SHA-256 الخاص بها قبل تحميلها.

******

### مكتبة FFmpeg

******

بدءا من الإصدار 1.1.0 يمكن للإضافة تنزيل مكتبة FFmpeg اختيارية مبنية بموجب LGPL-2.1-or-later. المكتبة ليست جزءا من ملف APK: تُنزَّل لواجهة ABI في جهازك فقط عند طلبك, وتُحمَّل ديناميكيا, وترافقها إشعارات الترخيص الخاصة بها وموقع الحصول على الشيفرة المصدرية المقابلة. ومن دونها تبقى جميع ميزات الإصدار 1.0.0 متاحة.

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

_2026/11/05_

- `تلميح` معاينة للتطوير: يستطيع مركز الإضافات في AutoJs6 اكتشاف الإضافة, ولم تُسلَّم بعد ميزات المعالجة وشاشات Studio وطرق المعالجة في وحدتي السكربت video و audio
- `تلميح` يتطلب AutoJs6 6.8.1 (6641) أو أحدث (سيُحدَّد الحد الأدنى الدقيق للإصدار بعد اكتمال تغييرات المضيف)
- `ميزة` هيكل الإضافة: بروتوكول التفعيل Wake, وخدمة الهوية في مركز الإضافات, وخدمة قدرات معالجة الوسائط (إصدار العقد 1)
- `ميزة` شاشة رئيسية لمنح الوصول إلى جميع الملفات
- `ميزة` ملف APK واحد لجميع واجهات ABI: المكتبة الأصلية الوحيدة تأتي من Jetpack Compose وهي محاذاة على صفحات ذاكرة بحجم 16 KB
- `ميزة` ملفات README وتعليمات مركز الإضافات وسجلات التغييرات بعشر لغات, مولَّدة من مصادر JSON
- `تبعية` إرفاق Jetpack Compose BOM 2026.09.00 (شاشات الإضافة)
- `تبعية` إرفاق Navigation Compose 2.10.2
- `تبعية` إرفاق Jetpack Media3 1.10.1 (Transformer و Effect و Muxer و ExoPlayer و UI للمعالجة والمعاينة)
- `تبعية` إرفاق AndroidX Core KTX 1.18.0

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
