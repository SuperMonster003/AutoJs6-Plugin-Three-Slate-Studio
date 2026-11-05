******

### Release history

******

# v1.0.0

###### 2026/11/05

* `Hint` Development preview: the AutoJs6 Plugin Center can discover the plugin; the processing features, the Studio screens and the processing methods of the video and audio script modules are not delivered yet
* `Hint` Requires AutoJs6 6.8.1 (6641) or later (the exact minimum build will be filled in once the host changes land)
* `Feature` Plugin skeleton: Wake activation protocol, Plugin Center identity service and media studio capability service (contract version 1)
* `Feature` Home screen with the all files access grant
* `Feature` A single APK for every ABI: the only native library comes from Jetpack Compose and is aligned to 16 KB memory pages
* `Feature` README, Plugin Center instructions and changelogs in 10 languages, generated from JSON sources
* `Dependency` Attach Jetpack Compose BOM 2026.09.00 (the plugin screens)
* `Dependency` Attach Navigation Compose 2.10.2
* `Dependency` Attach Jetpack Media3 1.10.1 (Transformer, Effect, Muxer, ExoPlayer and UI for processing and previews)
* `Dependency` Attach AndroidX Core KTX 1.18.0
