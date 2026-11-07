3-Slate Studio est le plugin vidéo et audio d'AutoJs6 et aussi une application de montage qui fonctionne sans AutoJs6. Il découpe, assemble, accélère, redimensionne, fait pivoter, recadre et filigrane les vidéos, extrait des images, crée des GIF et transcode; il découpe, assemble, mixe, applique des fondus et convertit l'audio; et son éditeur de timeline multipiste combine des clips avec des transitions, des incrustations et des sous-titres.

> La version 1.0.0 est la première version publiée. Traiter des médias dans les scripts nécessite AutoJs6 6.8.1 (version interne 6641) ou ultérieure; les écrans du plugin, l'éditeur de timeline et l'enregistreur Studio ne dépendent pas d'AutoJs6.

### Points Forts

- Montage vidéo: découpe par temps, assemblage, changement de vitesse (de 0.25x à 8x), redimensionnement, rotation, recadrage et retournement, filigranes texte ou image, extraction ou suppression de la piste audio, et transcodage (H.264 par défaut, H.265 si l'appareil dispose d'un encodeur H.265).
- Images et GIF: images isolées, séquences d'images et miniatures sous forme de fichiers PNG ou JPG ou d'objets `Image`, et animations GIF produites par l'encodeur intégré.
- Audio: découpe, assemblage, mixage de plusieurs entrées (réduit automatiquement lorsque la somme saturerait), réglage du volume avec fondu d'entrée et de sortie, changement de vitesse, et conversion en M4A, WAV, FLAC ou OGG (OGG exige Android 10 ou ultérieur).
- Éditeur de timeline multipiste: pistes vidéo, audio, d'incrustation et de sous-titres, 7 transitions (coupe, fondu, fondu enchaîné, glissement, volet, zoom, flou), aperçu en direct, annulation et rétablissement, et projets enregistrés au format `.slate.json`; les sous-titres gèrent l'import et l'export SRT et un sous-ensemble de base d'ASS, incrustés dans l'image à l'export.
- Boîte à outils: un assistant pour chaque fonction de traitement, avec des formulaires qui indiquent les valeurs par défaut et les plages; les tâches s'exécutent dans une file qui affiche la progression et la cause des échecs, et les longues tâches gardent une notification de premier plan.
- Génération de code: chaque assistant, l'export de la timeline et l'enregistreur produisent le script `video.*` / `audio.*` équivalent, prêt à être copié ou ouvert comme nouveau script dans AutoJs6.
- Enregistreur Studio: enregistre le micro, l'audio interne de l'appareil (Android 10 ou ultérieur) ou les deux depuis les écrans du plugin, en M4A ou en WAV, avec une pause et un enregistrement qui continue écran éteint.
- Bibliothèque FFmpeg (prévue pour la 1.1.0): un téléchargement facultatif qui ajoute des formats comme MP3, WebM et MKV et le rendu complet des sous-titres ASS.

L'avancement et la conception sont disponibles sur la [page du projet](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Slate-Studio).
