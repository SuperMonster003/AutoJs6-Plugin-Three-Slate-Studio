<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-slate-studio-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Traitement vidéo et audio, montage multipiste sur une timeline, transitions et sous-titres, avec du code de script généré depuis les écrans</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Langues (Languages)

******

Le README.md actuel prend en charge les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Slate Studio est le plugin vidéo et audio d'AutoJs6 et aussi une application de montage qui fonctionne sans AutoJs6. Il découpe, assemble, accélère, redimensionne, fait pivoter, recadre et filigrane les vidéos, extrait des images, crée des GIF et transcode; il découpe, assemble, mixe, applique des fondus et convertit l'audio; et son éditeur de timeline multipiste combine des clips avec des transitions, des incrustations et des sous-titres.

Le traitement s'exécute dans le plugin avec Jetpack Media3 Transformer et les codecs matériels de l'appareil, si bien que l'hôte AutoJs6 reste léger. Les scripts AutoJs6 traitent les médias avec les modules globaux `video` et `audio`; l'enregistrement d'écran (`video.record`) et l'enregistrement audio (`audio.record`) sont intégrés à AutoJs6 et fonctionnent sans le plugin. Chaque écran d'outil du plugin affiche le code de script équivalent, que l'on peut copier ou ouvrir comme nouveau script dans AutoJs6.

> La version 1.0.0 est la première version publiée. Traiter des médias dans les scripts nécessite AutoJs6 6.8.1 (version interne 6641) ou ultérieure; les écrans du plugin, l'éditeur de timeline et l'enregistreur Studio ne dépendent pas d'AutoJs6.

******

### Points Forts

******

- Montage vidéo: découpe par temps, assemblage, changement de vitesse (de 0.25x à 8x), redimensionnement, rotation, recadrage et retournement, filigranes texte ou image, extraction ou suppression de la piste audio, et transcodage (H.264 par défaut, H.265 si l'appareil dispose d'un encodeur H.265).
- Images et GIF: images isolées, séquences d'images et miniatures sous forme de fichiers PNG ou JPG ou d'objets `Image`, et animations GIF produites par l'encodeur intégré.
- Audio: découpe, assemblage, mixage de plusieurs entrées (réduit automatiquement lorsque la somme saturerait), réglage du volume avec fondu d'entrée et de sortie, changement de vitesse, et conversion en M4A, WAV, FLAC ou OGG (OGG exige Android 10 ou ultérieur).
- Éditeur de timeline multipiste: pistes vidéo, audio, d'incrustation et de sous-titres, 7 transitions (coupe, fondu, fondu enchaîné, glissement, volet, zoom, flou), aperçu en direct, annulation et rétablissement, et projets enregistrés au format `.slate.json`; les sous-titres gèrent l'import et l'export SRT et un sous-ensemble de base d'ASS, incrustés dans l'image à l'export.
- Boîte à outils: un assistant pour chaque fonction de traitement, avec des formulaires qui indiquent les valeurs par défaut et les plages; les tâches s'exécutent dans une file qui affiche la progression et la cause des échecs, et les longues tâches gardent une notification de premier plan.
- Génération de code: chaque assistant, l'export de la timeline et l'enregistreur produisent le script `video.*` / `audio.*` équivalent, prêt à être copié ou ouvert comme nouveau script dans AutoJs6.
- Enregistreur Studio: enregistre le micro, l'audio interne de l'appareil (Android 10 ou ultérieur) ou les deux depuis les écrans du plugin, en M4A ou en WAV, avec une pause et un enregistrement qui continue écran éteint.
- Bibliothèque FFmpeg (prévue pour la 1.1.0): un téléchargement facultatif qui ajoute des formats comme MP3, WebM et MKV et le rendu complet des sous-titres ASS.

******

### Captures D'Écran

******

Ces captures proviennent de téléphones sous Android 12 et 13, avec le thème clair et l'interface en chinois simplifié.

| Médiathèque | Boîte à outils | Éditeur de timeline | Enregistreur Studio |
| :---: | :---: | :---: | :---: |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/library.png?raw=true" alt="library" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/toolbox.png?raw=true" alt="toolbox" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/timeline.png?raw=true" alt="timeline" width="200" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/docs/images/screenshots/recorder.png?raw=true" alt="recorder" width="200" /> |

******

### Utilisation

******

1. Installez 3-Slate Studio depuis le centre des plugins d'AutoJs6, ou téléchargez l'APK depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) et installez-le. Il n'existe qu'un seul paquet pour toutes les ABI.
2. Ouvrez le plugin et accordez l'accès à tous les fichiers lorsqu'il est demandé, afin qu'il puisse lire et écrire les chemins que vous choisissez.
3. Pour utiliser les méthodes de traitement de `video` et `audio` dans les scripts, utilisez AutoJs6 6.8.1 (version interne 6641) ou ultérieure et activez le plugin dans le centre des plugins.

> Le plugin prend en charge les appareils sous Android 7.0 (API 24) ou ultérieur. Sa seule bibliothèque native provient de Jetpack Compose et est alignée sur des pages mémoire de 16 KB, si bien qu'un seul paquet sert toutes les ABI.

******

### API De Script

******

Les scripts utilisent ces fonctions via les modules globaux `video` et `audio` (également disponibles sous les noms `$video` et `$audio`) (la documentation d'AutoJs6 de [video](https://docs.autojs6.com/#/video) et [audio](https://docs.autojs6.com/#/audio) décrit chaque méthode et option):

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

- Chaque méthode de traitement existe sous une forme bloquante et sous une forme `xxxAsync` qui renvoie une Promise; `options.onProgress(progress)` indique `percent`, `stage` et `etaMillis`, et l'arrêt du script annule ses tâches.
- Si `output` est omis, les vidéos vont dans `Movies/3-Slate Studio/`, l'audio dans `Music/3-Slate Studio/`, et les images extraites, miniatures et GIF dans `Pictures/3-Slate Studio/`, avec un nom formé de la source, de l'opération et de l'heure; les résultats contiennent le chemin réel et les informations du média et sont enregistrés dans la médiathèque.
- `video.compose(timeline)` effectue le rendu d'une timeline multipiste décrite par un objet (jusqu'à 8 pistes vidéo et 16 pistes audio, clips, transitions, incrustations, sous-titres), au même format que l'export de l'éditeur de timeline.
- `video.record` et `audio.record` sont enregistrés par AutoJs6 et vont dans `Movies/AutoJs6/` et `Music/AutoJs6/` si le chemin est omis; l'enregistrement d'écran exige d'abord `images.requestScreenCapture`.
- `video.tasks()` / `audio.tasks()` listent et annulent les tâches en cours du script; les échecs lèvent une `MediaStudioError` avec un `code` stable comme `INVALID_OPTIONS` ou `STORAGE_PERMISSION_DENIED`.

******

### Scripts D'Exemple

******

Le dossier [samples](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/tree/master/samples) de ce dépôt contient 4 scripts prêts à l'emploi, et les exemples fournis avec AutoJs6 contiennent les mêmes scripts (dossier 3-Slate Studio):

- Enregistrer l'écran 10 secondes et en faire un GIF: `video.record` enregistre l'écran sans son, puis `video.toGif` le convertit en GIF de 10 images par seconde et 480 pixels de large (la première exécution demande de confirmer la capture d'écran).
- Enregistrer de l'audio et le convertir en FLAC: `audio.record` enregistre le micro, puis `audio.convert` le convertit en FLAC sans perte (la première exécution demande l'autorisation d'enregistrement).
- Découper, filigraner et assembler: `video.probe` lit la source, `video.trim` garde les 5 premières secondes, `video.overlay` ajoute un filigrane texte et `video.concat` assemble les deux parties en indiquant la progression.
- Composition de timeline: `video.compose` combine deux vidéos (avec un fondu enchaîné), une musique de fond (avec des fondus) et une incrustation de texte en une vidéo verticale.

Les deux derniers exemples lisent par défaut `/sdcard/Movies/a.mp4`, `/sdcard/Movies/b.mp4` et `/sdcard/Music/bgm.m4a`; placez-y vos médias ou changez les chemins.

******

### Compatibilité

******

Le plugin prend en charge Android 7.0 (API 24) ou ultérieur et fonctionne sans AutoJs6. L'activer dans le centre des plugins et utiliser les méthodes de traitement de `video` et `audio` dans les scripts nécessite AutoJs6 6.8.1 (version interne 6641) ou ultérieure (F10); les deux modules fonctionnent dans les moteurs Rhino, Node.js et Bun.

Les numéros entre parenthèses renvoient aux preuves de [l'annexe F de la feuille de route](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md). "Non testé" signifie que la version ou le format n'a pas été vérifié avant la sortie de la 1.0.0, et non qu'il ne fonctionne pas.

**Par version d'Android**

| Android | Appareils | Traitement | Écrans du plugin | Enregistreur Studio | Scripts d'exemple |
| --- | --- | --- | --- | --- | --- |
| 7.0 (API 24) | Émulateur Android | Réussi (F5) | Réussi (F4, F6) | Non testé | Non testé |
| 7.1 - 8.1 (API 25 - 27) | - | Non testé | Non testé | Non testé | Non testé |
| 9 (API 28) | Sony G8441 | Réussi (F3) | Écran d'enregistrement réussi (F8) | Réussi, micro seulement (F8) | Non testé |
| 10 - 11 (API 29 - 30) | - | Non testé | Non testé | Non testé | Non testé |
| 12 (API 31) | Sony XQ-AT72 | Réussi (F3, F5) | Réussi (F4, F5, F7) | Réussi, avec l'audio interne (F8) | Non testé |
| 12L (API 32) | - | Non testé | Non testé | Non testé | Non testé |
| 13 (API 33) | Sony XQ-DQ72, Redmi 22120RN86C | Réussi (F1, F3) | Réussi (F6, F8) | Réussi, avec l'audio interne (F8) | Réussi (F9) |
| 14 (API 34) | - | Non testé | Non testé | Non testé | Non testé |
| 15 (API 35) | Xiaomi Pad 23046RP50C, OnePlus OPD2413, Émulateur Android | Réussi (F1, F2, F3, F5) | Réussi (F4, F5, F6) | Réussi, avec l'audio interne (F8) | Non testé |

Traitement: les 25 opérations de script ou les 20 assistants de la boîte à outils; écrans du plugin: médiathèque, boîte à outils, tâches, réglages et éditeur de timeline; scripts d'exemple: les 4 exemples ci-dessus.

**Par format**

| Format | En entrée | En sortie | Remarques |
| --- | --- | --- | --- |
| MP4: vidéo H.264 + audio AAC | Réussi (F2, F3) | Réussi (F2, F3) | Sortie vidéo par défaut |
| MP4: vidéo H.265 / HEVC | Non testé | Réussi (F1, F2, F3) | Seulement si l'appareil dispose d'un encodeur H.265; les 6 téléphones et tablettes testés en ont un |
| M4A (AAC) | Réussi (F2, F3) | Réussi (F2, F3, F8) | Sortie audio par défaut et format par défaut de l'enregistreur Studio |
| WAV (PCM 16 bits) | Réussi (F2) | Réussi (F2, F3) | Sans perte, fichiers volumineux |
| FLAC | Non testé | Réussi (F2, F3) | Compression sans perte |
| OGG (Opus) | Non testé | Réussi (F2, F3) | Exige Android 10 ou ultérieur (le système écrit l'OGG depuis Android 10); testé sous Android 12 - 15 |
| GIF | Non testé | Réussi (F2, F3) | Encodeur intégré, 1 - 50 images par seconde |
| Images PNG, JPG | Réussi (F2, F3) | Réussi (F2, F3) | Entrée: images d'incrustation; sortie: images extraites et miniatures |
| Sous-titres SRT | Réussi (F2, F3, F6) | Réussi (F2) | Importés dans l'éditeur de timeline ou incrustés dans l'image |
| Sous-titres ASS (sous-ensemble de base) | Réussi (F2) | Réussi (F2, F3) | Les balises non prises en charge sont ignorées avec un avertissement; le rendu ASS complet est prévu pour la 1.1.0 |
| MP3, AMR et autres formats audio | Non testé | Absent de la 1.0.0 | L'entrée dépend des décodeurs système de l'appareil; la sortie MP3 est prévue avec la bibliothèque FFmpeg de la 1.1.0 |
| WebM, MKV | Non testé | Absent de la 1.0.0 | Prévu avec la bibliothèque FFmpeg de la 1.1.0 |

******

### FAQ

******

#### Puis-je l'utiliser sans AutoJs6?

Oui. La médiathèque, la boîte à outils, l'éditeur de timeline et l'enregistreur Studio fonctionnent seuls. AutoJs6 n'est nécessaire que lorsque des scripts traitent des médias.

#### Pourquoi le plugin demande-t-il l'accès à tous les fichiers?

Les scripts transmettent des chemins de fichiers ordinaires comme `/sdcard/DCIM/...`, et le plugin les lit et les écrit directement, comme 3-Maple Image. Sans l'autorisation, le traitement renvoie `STORAGE_PERMISSION_DENIED` et l'écran du plugin explique comment l'accorder. `video.frame` renvoie un objet `Image`, mais l'image passe tout de même à AutoJs6 par un fichier temporaire dans `Movies/3-Slate Studio/`, elle exige donc la même autorisation.

#### L'enregistrement d'écran nécessite-t-il le plugin?

Non. `video.record` et `audio.record` sont intégrés à AutoJs6 (6.8.1 ou ultérieure); le plugin ajoute les méthodes de traitement et son propre enregistreur Studio.

#### Puis-je l'utiliser avec une version plus ancienne d'AutoJs6?

Les scripts ont besoin d'AutoJs6 6.8.1 (version interne 6641) ou ultérieure pour utiliser le plugin; les versions plus anciennes n'ont pas encore les modules `video` et `audio`. Le plugin lui-même reste utilisable comme application de montage autonome.

#### L'export de la timeline renvoie `CODEC_ERROR` et demande moins de pistes. Que faire?

Pendant une transition avec chevauchement (fondu enchaîné, glissement, volet, zoom, flou), une piste vidéo décode deux clips à la fois, et un appareil n'a qu'un nombre limité de codecs. Les trois téléphones Sony testés exportent 6 pistes vidéo avec transitions plus 16 pistes audio, tandis que 8 + 16 se termine par `CODEC_ERROR`; utilisez moins de pistes vidéo ou moins de transitions avec chevauchement.

******

### Permissions Et Sécurité

******

Autorisations demandées par le plugin et leur usage:

- Accès à tous les fichiers (`MANAGE_EXTERNAL_STORAGE`; les autorisations de stockage sous Android 10 et antérieur): lit et écrit les chemins choisis par les scripts et les écrans, et écrit les sorties sans chemin dans les dossiers `3-Slate Studio` sous `Movies`, `Music` et `Pictures`.
- Accès aux médias (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, Android 13 et ultérieur): liste les vidéos et l'audio de l'appareil dans la médiathèque de l'écran d'accueil.
- Micro (`RECORD_AUDIO`): utilisé uniquement par l'enregistreur Studio; `audio.record` et `video.record` dans les scripts sont enregistrés par AutoJs6 avec sa propre autorisation d'enregistrement.
- Capture d'écran (le consentement de partage d'écran du système, pas une autorisation du manifeste): nécessaire lorsque l'enregistreur Studio capture l'audio interne (audio interne ou mixte); le système le demande au début de l'enregistrement.
- Notifications (`POST_NOTIFICATIONS`, Android 13 et ultérieur): affichent la progression du traitement et la notification d'enregistrement avec les boutons pause et arrêt; sans elles, ces notifications ne s'affichent pas.
- Services de premier plan et wakelock (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_MEDIA_PROJECTION`, `WAKE_LOCK`): poursuivent les longs traitements en arrière-plan et l'enregistrement écran éteint.
- Réseau (`INTERNET`, `ACCESS_NETWORK_STATE`): dans la 1.0.0, uniquement pour la vérification des mises à jour, qui lit l'index officiel des plugins au plus une fois toutes les 12 heures à l'ouverture de l'écran d'accueil et peut être désactivée dans les réglages.
- Autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`): communique avec AutoJs6.

Le plugin réduit autant que possible ses autorisations:

- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement.
- Stockage: l'accès à tous les fichiers ne sert qu'aux chemins choisis par les scripts et les écrans Studio; les sorties sans chemin vont uniquement dans les dossiers `3-Slate Studio` sous `Movies`, `Music` et `Pictures`.
- Enregistrement: le micro et l'audio interne de l'appareil ne sont capturés que pendant que l'enregistreur Studio fonctionne, avec une notification de premier plan; la capture de l'audio interne exige aussi le consentement de capture d'écran du système.
- Réseau: la version 1.0.0 ne fait que vérifier les mises à jour; la bibliothèque FFmpeg prévue (1.1.0) ne sera téléchargée que si vous la demandez et son SHA-256 sera vérifié avant le chargement.

******

### Bibliothèque FFmpeg

******

La bibliothèque FFmpeg est prévue pour la version 1.1.0 et sera compilée sous LGPL-2.1-or-later. Elle ne fera pas partie de l'APK: elle ne sera téléchargée pour votre ABI que lorsque vous la demanderez, sera chargée dynamiquement et sera accompagnée de ses propres mentions de licence et de l'emplacement du code source correspondant. Sans elle, toutes les fonctions de la 1.0.0 restent disponibles. Les formats et l'interface de script de la bibliothèque seront décrits à la sortie de la 1.1.0.

> Certains codecs pris en charge par FFmpeg, comme H.265 / HEVC et AAC, peuvent être couverts par des brevets dans certains pays ou régions. La bibliothèque est proposée uniquement par commodité et n'inclut aucune licence de brevet; vérifiez les règles qui vous concernent avant de l'utiliser.

******

### Interface Du Plugin

******

Les informations suivantes s'adressent aux développeurs de l'hôte AutoJs6 et de plugins; l'hôte utilise ces identifiants pour découvrir le plugin et négocier les capacités:

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

### Feuille De Route

******

Les plans, décisions de conception et l'avancement sont suivis sous forme de liste à cocher dans ROADMAP.md. Les éléments non cochés sont des intentions, pas des fonctionnalités de la version actuelle.

- [Voir ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/ROADMAP.md)

******

### Historique Des Versions

******

#### v1.0.0

_2026/11/08_

- `Note` Traiter des médias avec video / audio dans les scripts nécessite AutoJs6 6.8.1 (version interne 6641) ou ultérieure; les écrans du plugin, l'éditeur de timeline et l'enregistreur Studio ne dépendent pas d'AutoJs6
- `Note` L'API de script est décrite dans les chapitres [video](https://docs.autojs6.com/#/video) et [audio](https://docs.autojs6.com/#/audio) de la documentation d'AutoJs6
- `Fonctionnalité` Traitement vidéo: découpe, assemblage, vitesse (0.25x - 8x), redimensionnement, rotation, recadrage et retournement, filigranes texte ou image, extraction ou suppression de la piste audio, et transcodage (H.264, ou H.265 si l'appareil le prend en charge)
- `Fonctionnalité` Images extraites, miniatures et GIF: fichiers PNG / JPG ou objets Image, et un encodeur GIF intégré (palette, tramage, boucle)
- `Fonctionnalité` Traitement audio: découpe, assemblage, mixage (réduit automatiquement s'il saturerait), volume et fondus, vitesse, et conversion en M4A / WAV / FLAC / OGG (OGG exige Android 10+)
- `Fonctionnalité` Composition multipiste avec video.compose: jusqu'à 8 pistes vidéo et 16 pistes audio, 7 transitions, incrustations texte / image et sous-titres
- `Fonctionnalité` Sous-titres: lecture et écriture SRT et un sous-ensemble de base d'ASS, incrustés dans l'image avec video.burnSubtitles, et video.subtitles.parse / write pour les fichiers de sous-titres
- `Fonctionnalité` Modules de script AutoJs6 video / audio (AutoJs6 6.8.1, Rhino, Node.js et Bun): chaque méthode de traitement a une forme xxxAsync et un rappel de progression, et l'arrêt du script annule ses tâches
- `Fonctionnalité` Écrans Studio: médiathèque, boîte à outils, file des tâches et réglages; l'apparence suit AutoJs6 ou utilise sa propre langue, son mode nuit et sa couleur de thème, et les réglages guident vers l'accès à tous les fichiers et l'utilisation de la batterie sans restriction
- `Fonctionnalité` Assistants de la boîte à outils: un assistant par fonction de traitement, avec des formulaires qui indiquent les valeurs par défaut et les plages, et une page de résultat avec la sortie, les informations du média et le script équivalent
- `Fonctionnalité` Éditeur de timeline: montage multipiste, transitions, édition des incrustations et des sous-titres, aperçu en direct, annulation / rétablissement, projets enregistrés au format .slate.json, et export dans la file des tâches
- `Fonctionnalité` Génération de code: les assistants, la timeline et l'enregistreur affichent le script complet équivalent, prêt à être copié ou ouvert dans AutoJs6 comme nouveau script
- `Fonctionnalité` Enregistreur Studio: micro, audio interne de l'appareil (Android 10+) ou les deux, M4A / WAV, pause / reprise, enregistrement écran éteint et commandes dans la notification
- `Fonctionnalité` Les longues tâches s'exécutent dans un service de premier plan avec une notification de progression; les réglages choisissent l'encodeur matériel préféré et le nombre de tâches simultanées (1 - 2)
- `Fonctionnalité` Les sorties sans chemin vont dans les dossiers 3-Slate Studio sous Movies, Music et Pictures et sont enregistrées dans la médiathèque
- `Fonctionnalité` Vérification des mises à jour: lit l'index officiel des plugins au plus une fois toutes les 12 heures à l'ouverture de l'écran d'accueil, et une version peut être ignorée
- `Fonctionnalité` Un seul APK pour toutes les ABI: la seule bibliothèque native provient de Jetpack Compose et est alignée sur des pages mémoire de 16 KB
- `Fonctionnalité` Écrans, README, instructions du centre des plugins et journal des modifications en 10 langues
- `Dépendance` Ajout de Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer et UI, pour le traitement et les aperçus)
- `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00 (écrans du plugin)
- `Dépendance` Ajout d'AndroidX Activity Compose 1.13.0
- `Dépendance` Ajout de Navigation Compose 2.10.2
- `Dépendance` Ajout d'AndroidX Core KTX 1.18.0
- `Dépendance` Ajout de Material Color Utilities (MaterialKolor) 4.1.1 (couleur de thème des écrans du plugin)

##### Pour plus d'historique des versions

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

Cette section s'adresse aux développeurs souhaitant compiler le plugin depuis les sources.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
.\gradlew.bat :app:appendDigestToReleasedFiles
py .python\generate_markdown.py --check
py .python\generate_icon_studio.py --check
py .python\generate_third_party_notices.py --check
py .python\check_repository.py
```

******

### Licence

******

Le code du projet est distribué sous [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/LICENSE). Les composants et données tiers sont répertoriés dans les mentions tierces.

- [Mentions tierces (THIRD_PARTY_NOTICES.md)](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/blob/master/THIRD_PARTY_NOTICES.md)

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
