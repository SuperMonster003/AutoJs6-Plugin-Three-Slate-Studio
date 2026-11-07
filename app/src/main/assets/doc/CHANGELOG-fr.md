******

### Historique Des Versions

******

# v1.0.0

###### 2026/11/08

* `Note` Traiter des médias avec video / audio dans les scripts nécessite AutoJs6 6.8.1 (version interne 6641) ou ultérieure; les écrans du plugin, l'éditeur de timeline et l'enregistreur Studio ne dépendent pas d'AutoJs6
* `Note` L'API de script est décrite dans les chapitres [video](https://docs.autojs6.com/#/video) et [audio](https://docs.autojs6.com/#/audio) de la documentation d'AutoJs6
* `Fonctionnalité` Traitement vidéo: découpe, assemblage, vitesse (0.25x - 8x), redimensionnement, rotation, recadrage et retournement, filigranes texte ou image, extraction ou suppression de la piste audio, et transcodage (H.264, ou H.265 si l'appareil le prend en charge)
* `Fonctionnalité` Images extraites, miniatures et GIF: fichiers PNG / JPG ou objets Image, et un encodeur GIF intégré (palette, tramage, boucle)
* `Fonctionnalité` Traitement audio: découpe, assemblage, mixage (réduit automatiquement s'il saturerait), volume et fondus, vitesse, et conversion en M4A / WAV / FLAC / OGG (OGG exige Android 10+)
* `Fonctionnalité` Composition multipiste avec video.compose: jusqu'à 8 pistes vidéo et 16 pistes audio, 7 transitions, incrustations texte / image et sous-titres
* `Fonctionnalité` Sous-titres: lecture et écriture SRT et un sous-ensemble de base d'ASS, incrustés dans l'image avec video.burnSubtitles, et video.subtitles.parse / write pour les fichiers de sous-titres
* `Fonctionnalité` Modules de script AutoJs6 video / audio (AutoJs6 6.8.1, Rhino, Node.js et Bun): chaque méthode de traitement a une forme xxxAsync et un rappel de progression, et l'arrêt du script annule ses tâches
* `Fonctionnalité` Écrans Studio: médiathèque, boîte à outils, file des tâches et réglages; l'apparence suit AutoJs6 ou utilise sa propre langue, son mode nuit et sa couleur de thème, et les réglages guident vers l'accès à tous les fichiers et l'utilisation de la batterie sans restriction
* `Fonctionnalité` Assistants de la boîte à outils: un assistant par fonction de traitement, avec des formulaires qui indiquent les valeurs par défaut et les plages, et une page de résultat avec la sortie, les informations du média et le script équivalent
* `Fonctionnalité` Éditeur de timeline: montage multipiste, transitions, édition des incrustations et des sous-titres, aperçu en direct, annulation / rétablissement, projets enregistrés au format .slate.json, et export dans la file des tâches
* `Fonctionnalité` Génération de code: les assistants, la timeline et l'enregistreur affichent le script complet équivalent, prêt à être copié ou ouvert dans AutoJs6 comme nouveau script
* `Fonctionnalité` Enregistreur Studio: micro, audio interne de l'appareil (Android 10+) ou les deux, M4A / WAV, pause / reprise, enregistrement écran éteint et commandes dans la notification
* `Fonctionnalité` Les longues tâches s'exécutent dans un service de premier plan avec une notification de progression; les réglages choisissent l'encodeur matériel préféré et le nombre de tâches simultanées (1 - 2)
* `Fonctionnalité` Les sorties sans chemin vont dans les dossiers 3-Slate Studio sous Movies, Music et Pictures et sont enregistrées dans la médiathèque
* `Fonctionnalité` Vérification des mises à jour: lit l'index officiel des plugins au plus une fois toutes les 12 heures à l'ouverture de l'écran d'accueil, et une version peut être ignorée
* `Fonctionnalité` Un seul APK pour toutes les ABI: la seule bibliothèque native provient de Jetpack Compose et est alignée sur des pages mémoire de 16 KB
* `Fonctionnalité` Écrans, README, instructions du centre des plugins et journal des modifications en 10 langues
* `Dépendance` Ajout de Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer et UI, pour le traitement et les aperçus)
* `Dépendance` Ajout de Jetpack Compose BOM 2026.09.00 (écrans du plugin)
* `Dépendance` Ajout d'AndroidX Activity Compose 1.13.0
* `Dépendance` Ajout de Navigation Compose 2.10.2
* `Dépendance` Ajout d'AndroidX Core KTX 1.18.0
* `Dépendance` Ajout de Material Color Utilities (MaterialKolor) 4.1.1 (couleur de thème des écrans du plugin)
