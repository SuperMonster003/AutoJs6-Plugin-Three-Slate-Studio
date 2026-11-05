3-Slate Studio est le plugin vidéo et audio d'AutoJs6 et aussi une application de montage qui fonctionne sans AutoJs6. Il découpe, assemble, accélère, redimensionne, fait pivoter, recadre et filigrane les vidéos, extrait des images, crée des GIF et transcode; il découpe, assemble, mixe, applique des fondus et convertit l'audio; et son éditeur de timeline multipiste combine des clips avec des transitions, des incrustations et des sous-titres.

> Il s'agit d'une préversion de développement: le centre des plugins d'AutoJs6 peut détecter le plugin, mais les fonctions de traitement, les écrans Studio et les méthodes de traitement des modules `video` et `audio` ne sont pas encore livrés. La suite décrit les objectifs de conception de chaque version; consultez la feuille de route pour l'avancement.

### Points Forts

- Montage vidéo: découpe par temps, assemblage, changement de vitesse (de 0.25x à 8x), redimensionnement, rotation, recadrage et retournement, filigranes texte ou image, extraction ou suppression de la piste audio, et transcodage (H.264 par défaut, H.265 si l'appareil le prend en charge).
- Images et GIF: images isolées, séquences d'images et miniatures sous forme de fichiers image ou d'objets `Image`, et animations GIF produites par l'encodeur intégré.
- Audio: découpe, assemblage, mixage de plusieurs entrées, réglage du volume avec fondu d'entrée et de sortie, changement de vitesse, et conversion en M4A, WAV, FLAC ou OGG.
- Timeline multipiste: pistes vidéo, audio et d'incrustation, 7 transitions (coupe, fondu, fondu enchaîné, glissement, volet, zoom, flou) et sous-titres avec import et export SRT et un sous-ensemble de base d'ASS, incrustés dans l'image à l'export.
- Génération de code: chaque écran d'outil et l'export de la timeline produisent le code `video.*` / `audio.*` équivalent, prêt à être copié ou ouvert comme nouveau script dans AutoJs6.
- Enregistreur Studio: enregistre le micro, l'audio interne de l'appareil (Android 10 ou ultérieur) ou les deux depuis les écrans du plugin.
- Bibliothèque FFmpeg (1.1.0): un téléchargement facultatif pour la sortie MP3, WebM et MKV, le passage direct de commandes avec `video.ffmpeg` et le rendu ASS complet.

L'avancement et la conception sont disponibles sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio).
