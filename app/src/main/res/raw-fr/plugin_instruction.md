3-Slate Studio est une application de montage vidéo et audio qui fonctionne aussi seule, sans AutoJs6. Elle coupe, assemble, accélère et transcode les vidéos, ajoute des filigranes ou crée des GIF; elle coupe, mixe et convertit aussi l'audio, et monte plusieurs clips en une vidéo sur une timeline, avec transitions et sous-titres. Avec AutoJs6, les scripts effectuent les mêmes traitements via les modules `video` et `audio`.

### Prise en main

1. Ouvrez 3-Slate Studio. Tant que l'accès n'est pas accordé, "Médiathèque" affiche "Accorder l'accès"; touchez-le et autorisez l'accès à tous les fichiers dans la page système qui s'ouvre.
2. Dans "Boîte à outils", choisissez un traitement, par exemple "Couper une vidéo", sélectionnez le média avec "Parcourir les fichiers" ou "Depuis la médiathèque", réglez les options et touchez "Lancer".
3. La progression s'affiche pendant le traitement; à la fin apparaissent les fichiers de sortie, les informations du média et le "Script équivalent". Toutes les tâches sont aussi listées dans "Tâches".
4. Avant de l'utiliser dans des scripts, vérifiez que ce plugin est activé dans le "Centre des plugins" d'AutoJs6.

### Fonctionnalités principales

- **Boîte à outils**: chaque traitement a son assistant, par exemple "Couper une vidéo", "Ajouter du texte et des images", "Convertir en GIF" et "Mixer des sons"; les formulaires indiquent les valeurs par défaut et les plages autorisées.
- **Montage sur timeline**: touchez "Nouveau projet de montage" dans "Boîte à outils" pour placer les clips sur les pistes vidéo, audio, superpositions et sous-titres, ajouter des transitions, prévisualiser en temps réel, annuler ou rétablir; "Exporter" place le résultat dans la file des tâches.
- **Sous-titres**: dans la timeline, "Importer des sous-titres" lit des fichiers SRT ou ASS, qui sont incrustés dans l'image à l'export; "Boîte à outils" propose aussi "Incruster des sous-titres" et "Convertir des sous-titres".
- **Enregistrement audio**: touchez le bouton "Enregistrer le son" dans la barre supérieure de l'accueil ou "Enregistrer le son" dans "Boîte à outils" pour enregistrer en M4A ou WAV depuis "Micro", "Son interne" ou "Mixte"; l'enregistrement peut être mis en pause et continue écran éteint.
- **Script équivalent**: les résultats des assistants, des exports de timeline et des enregistrements sont accompagnés d'un "Script équivalent", que vous pouvez "Copier" ou "Ouvrir dans AutoJs6" comme nouveau script non enregistré.
- **Médiathèque et tâches**: "Médiathèque" liste les médias des dossiers de sortie et du dossier d'enregistrement d'AutoJs6, avec aperçu, partage, renommage et "Traiter avec la boîte à outils"; "Tâches" affiche la progression et la cause d'échec de chaque tâche, et permet de l'annuler.
- **Paramètres**: "Paramètres" dans la barre supérieure règle l'apparence, le "Dossier de sortie", le "Codec vidéo par défaut", les "Tâches simultanées", etc.; le "Dossier de sortie" ne concerne que les assistants de la boîte à outils, les scripts gardent leurs propres sorties par défaut.

### Utilisation dans les scripts

Dans les scripts AutoJs6, le module global `video` traite les vidéos. L'exemple ci-dessous extrait 5 secondes de `./video.mp4` à partir de la 2e seconde, affiche la progression et convertit l'extrait en GIF:

```js
let clip = video.trim("./video.mp4", {
    start: 2000,
    duration: 5000,
    onProgress: (p) => console.log(p.stage, p.percent),
});
let gif = video.toGif(clip.path, { fps: 10, width: 480 });
console.log(gif.path, gif.sizeBytes);
```

Le module `audio` traite le son. L'exemple ci-dessous mixe une voix avec une musique de fond moins forte, puis convertit le résultat en FLAC:

```js
let mixed = audio.mix([
    { src: "./voice.m4a" },
    { src: "./music.m4a", volume: 0.3 },
]);
let flac = audio.convert(mixed.path, { format: "flac" });
console.log(flac.path, flac.durationMillis);
```

- Chaque méthode de traitement a aussi une forme `Async` qui renvoie une Promise, par exemple `video.trimAsync`; les formes synchrones ne peuvent pas être appelées sur le thread UI. Arrêter un script annule ses tâches non terminées.
- Les chemins relatifs sont résolus par rapport au répertoire de travail du script. Sans chemin de sortie, les résultats sont enregistrés selon leur type dans le dossier 3-Slate Studio de `Movies`, `Music` ou `Pictures`.
- L'enregistrement de l'écran avec `video.record` et l'enregistrement audio avec `audio.record` sont assurés par AutoJs6 et ne nécessitent pas ce plugin; les compositions multipistes utilisent `video.compose`, qui partage le format de projet du montage sur timeline.
- Toutes les méthodes et options sont décrites dans les sections [video](https://docs.autojs6.com/#/video) et [audio](https://docs.autojs6.com/#/audio) de la documentation AutoJs6.

### Prérequis

- Android 7.0 ou version ultérieure. Les écrans du plugin, le montage sur timeline et l'enregistrement audio n'ont pas besoin d'AutoJs6.
- Le traitement dans les scripts nécessite AutoJs6 6.8.1 ou version ultérieure, avec ce plugin activé dans le "Centre des plugins".
- L'accès à tous les fichiers sert à lire et écrire les fichiers indiqués par les scripts et les écrans, ainsi que les dossiers de sortie par défaut.
- L'autorisation du micro sert à l'enregistrement dans les écrans du plugin (le son interne en a aussi besoin); "Son interne" et "Mixte" nécessitent Android 10 ou version ultérieure, et le système vous demande d'abord d'autoriser la diffusion de l'écran.
- L'autorisation des notifications sert à afficher la progression des longues tâches et les commandes d'enregistrement dans le volet des notifications.

### Questions fréquentes

**Raisons pour lesquelles l'accès à tous les fichiers est nécessaire**

Le plugin lit et écrit directement les fichiers aux chemins indiqués par les scripts ou les écrans, et les dossiers de sortie par défaut se trouvent dans le stockage partagé. Sans cet accès, le traitement renvoie `STORAGE_PERMISSION_DENIED` et ouvre la page d'autorisation.

**Cas où l'export d'une timeline renvoie `CODEC_ERROR`**

Un appareil ne peut utiliser qu'un nombre limité de codecs à la fois, et les transitions superposées décodent deux vidéos en même temps. Réduisez le nombre de pistes vidéo ou de transitions superposées, ou réglez "Tâches simultanées" sur 1 dans "Paramètres".

**Cas où les longues tâches sont retardées ou l'enregistrement s'arrête écran éteint**

L'optimisation de la batterie ou les restrictions en arrière-plan peuvent retarder les longues tâches ou interrompre l'enregistrement écran éteint. Touchez "Utilisation de la batterie" dans "Paramètres", puis choisissez Non restreinte (ou Autoriser l'activité en arrière-plan) pour la batterie dans la page d'informations de l'application qui s'ouvre.

**Raisons pour lesquelles certaines applications sont muettes dans le son interne**

Une application peut interdire aux autres de capturer le son qu'elle lit; elle reste alors muette dans le son interne. Utilisez "Micro" pour enregistrer ces sons.

Les écrans, les autorisations et les scripts équivalents sont détaillés dans la section [3-Slate Studio](https://docs.autojs6.com/#/threeSlateStudio) de la documentation AutoJs6.
