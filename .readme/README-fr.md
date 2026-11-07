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

> Il s'agit d'une préversion de développement: le centre des plugins d'AutoJs6 peut détecter le plugin, mais les fonctions de traitement, les écrans Studio et les méthodes de traitement des modules `video` et `audio` ne sont pas encore livrés. La suite décrit les objectifs de conception de chaque version; consultez la feuille de route pour l'avancement.

******

### Points Forts

******

- Montage vidéo: découpe par temps, assemblage, changement de vitesse (de 0.25x à 8x), redimensionnement, rotation, recadrage et retournement, filigranes texte ou image, extraction ou suppression de la piste audio, et transcodage (H.264 par défaut, H.265 si l'appareil le prend en charge).
- Images et GIF: images isolées, séquences d'images et miniatures sous forme de fichiers image ou d'objets `Image`, et animations GIF produites par l'encodeur intégré.
- Audio: découpe, assemblage, mixage de plusieurs entrées, réglage du volume avec fondu d'entrée et de sortie, changement de vitesse, et conversion en M4A, WAV, FLAC ou OGG.
- Timeline multipiste: pistes vidéo, audio et d'incrustation, 7 transitions (coupe, fondu, fondu enchaîné, glissement, volet, zoom, flou) et sous-titres avec import et export SRT et un sous-ensemble de base d'ASS, incrustés dans l'image à l'export.
- Génération de code: chaque écran d'outil et l'export de la timeline produisent le code `video.*` / `audio.*` équivalent, prêt à être copié ou ouvert comme nouveau script dans AutoJs6.
- Enregistreur Studio: enregistre le micro, l'audio interne de l'appareil (Android 10 ou ultérieur) ou les deux depuis les écrans du plugin.
- Bibliothèque FFmpeg (1.1.0): un téléchargement facultatif pour la sortie MP3, WebM et MKV, le passage direct de commandes avec `video.ffmpeg` et le rendu ASS complet.

******

### Utilisation

******

1. Téléchargez l'APK depuis [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio/releases) et installez-le. Il n'existe qu'un seul paquet pour toutes les ABI.
2. Ouvrez le plugin et accordez l'accès à tous les fichiers lorsqu'il est demandé, afin qu'il puisse lire et écrire les chemins que vous choisissez.
3. Pour utiliser les méthodes de traitement de `video` et `audio` dans les scripts, mettez à jour AutoJs6 vers la version interne 6641 (6.8.1) ou ultérieure et activez le plugin dans le centre des plugins.

> Le plugin prend en charge les appareils sous Android 7.0 (API 24) ou ultérieur. Sa seule bibliothèque native provient de Jetpack Compose et est alignée sur des pages mémoire de 16 KB, si bien qu'un seul paquet sert toutes les ABI.

******

### API De Script

******

Les scripts utilisent ces fonctions via les modules globaux `video` et `audio` (également disponibles sous les noms `$video` et `$audio`) (projet de conception de la 1.0.0; la documentation de la version publiée fait foi):

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

- Chaque méthode de traitement existe sous une forme bloquante et sous une forme `xxxAsync` qui renvoie une Promise; `options.onProgress(progress)` indique `percent`, `stage` et `etaMillis`, et l'arrêt du script annule ses tâches.
- Si `output` est omis, les vidéos vont dans `Movies/3-Slate Studio/` et l'audio dans `Music/3-Slate Studio/`, avec un nom formé de la source, de l'opération et de l'heure; les résultats contiennent le chemin réel et les informations du média et sont enregistrés dans la médiathèque.
- `video.compose(timeline)` effectue le rendu d'une timeline multipiste décrite par un objet (pistes, clips, transitions, incrustations, sous-titres), au même format que l'export de l'éditeur de timeline.
- `video.tasks()` / `audio.tasks()` listent et annulent les tâches en cours du script; les échecs lèvent une `MediaStudioError` avec un `code` stable comme `INVALID_OPTIONS` ou `STORAGE_PERMISSION_DENIED`.

******

### FAQ

******

#### Puis-je l'utiliser sans AutoJs6?

Oui. Les écrans d'outils, l'éditeur de timeline et l'enregistreur Studio fonctionnent seuls. AutoJs6 n'est nécessaire que lorsque des scripts traitent des médias.

#### Pourquoi le plugin demande-t-il l'accès à tous les fichiers?

Les scripts transmettent des chemins de fichiers ordinaires comme `/sdcard/DCIM/...`, et le plugin les lit et les écrit directement, comme 3-Maple Image. Sans l'autorisation, le traitement renvoie `STORAGE_PERMISSION_DENIED` et l'écran du plugin explique comment l'accorder.

#### L'enregistrement d'écran nécessite-t-il le plugin?

Non. `video.record` et `audio.record` sont intégrés à AutoJs6 (version interne 6641 ou ultérieure); le plugin ajoute les méthodes de traitement et son propre enregistreur Studio.

******

### Permissions Et Sécurité

******

Le plugin réduit autant que possible ses autorisations:

- Entrées protégées: le service de capacité et l'entrée d'activation exigent l'autorisation de plugin d'AutoJs6 (`org.autojs.permission.PLUGIN`), les autres applications ne peuvent donc pas les appeler directement.
- Stockage: l'accès à tous les fichiers ne sert qu'aux chemins choisis par les scripts et les écrans Studio; les sorties sans chemin vont dans les dossiers du plugin sous `Movies` et `Music`.
- Enregistrement: le micro et l'audio interne de l'appareil ne sont capturés que pendant que l'enregistreur Studio fonctionne, avec une notification de premier plan; la capture de l'audio interne exige aussi le consentement de capture d'écran du système.
- Réseau: la version 1.0.0 ne fait que vérifier les mises à jour; à partir de la 1.1.0, la bibliothèque FFmpeg est téléchargée quand vous la demandez et son SHA-256 est vérifié avant le chargement.

******

### Bibliothèque FFmpeg

******

À partir de la version 1.1.0, le plugin peut télécharger une bibliothèque FFmpeg facultative compilée sous LGPL-2.1-or-later. La bibliothèque ne fait pas partie de l'APK: elle n'est téléchargée pour votre ABI que lorsque vous la demandez, elle est chargée dynamiquement et elle est accompagnée de ses propres mentions de licence et de l'emplacement du code source correspondant. Sans elle, toutes les fonctions de la 1.0.0 restent disponibles.

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

_2026/11/05_

- `Note` Préversion de développement: le centre des plugins d'AutoJs6 peut détecter le plugin; les fonctions de traitement, les écrans Studio et les méthodes de traitement des modules de script video et audio ne sont pas encore livrés
- `Note` Nécessite AutoJs6 6.8.1 (6641) ou ultérieur (la version minimale exacte sera indiquée une fois les modifications de l'hôte intégrées)
- `Fonctionnalité` Squelette du plugin: protocole d'activation Wake, service d'identité du centre des plugins et service de capacité de traitement multimédia (version de contrat 2)
- `Fonctionnalité` Écran d'accueil permettant d'accorder l'accès à tous les fichiers
- `Fonctionnalité` Un seul APK pour toutes les ABI: la seule bibliothèque native provient de Jetpack Compose et est alignée sur des pages mémoire de 16 KB
- `Fonctionnalité` README, instructions du centre des plugins et journaux des modifications en 10 langues, générés à partir de sources JSON
- `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00 (écrans du plugin)
- `Dépendance` Ajout de Navigation Compose 2.10.2
- `Dépendance` Ajout de Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer et UI pour le traitement et les aperçus)
- `Dépendance` Ajout d'AndroidX Core KTX 1.18.0

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
