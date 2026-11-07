// Generated from OptionSchemas of 3-Slate Studio (roadmap P4.2, D36). Do not edit by hand: change the schemas,
// then copy app/build/declarations/media-studio-options.d.ts here (OptionSchemasTest writes it when they differ).
// Times are milliseconds; null or a left-out key takes the default.

declare namespace MediaStudioOptions {

    // video.trim(src: string, options?: VideoTrimOptions, output?: string | null)
    interface VideoTrimOptions {
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        start?: number;
        /** Integer 1 - 9007199254740992 ms. Excludes the other length keys. */
        end?: number;
        /** Integer 1 - 9007199254740992 ms. Excludes the other length keys. */
        duration?: number;
    }

    // video.concat(sources: string[], options?: VideoConcatOptions, output?: string | null)
    interface VideoConcatOptions {
        /** Integer 16 - 8192 px. */
        width?: number;
        /** Integer 16 - 8192 px. */
        height?: number;
        /** Default contain. */
        fit?: 'contain' | 'cover' | 'stretch';
        /** Default h264. */
        codec?: 'h264' | 'h265';
        /** Integer 100000 - 200000000 bps. */
        bitRate?: number;
    }

    // video.speed(src: string, options: VideoSpeedOptions, output?: string | null)
    interface VideoSpeedOptions {
        /** 0.25 - 8 x. */
        factor: number;
        /** Default true. */
        keepPitch?: boolean;
        /** Default h264. */
        codec?: 'h264' | 'h265';
        /** Integer 100000 - 200000000 bps. */
        bitRate?: number;
    }

    // video.transform(src: string, options?: VideoTransformOptions, output?: string | null)
    interface VideoTransformOptions {
        /** Integer 16 - 8192 px. Excludes the other size keys. */
        width?: number;
        /** Integer 16 - 8192 px. Excludes the other size keys. */
        height?: number;
        /** Greater than 0, at most 8 x. Excludes the other size keys. */
        scale?: number;
        /** Integer -9007199254740992 - 9007199254740992 deg. multiples of 90, clockwise. Default 0. */
        rotate?: number;
        crop?: VideoTransformCrop;
        /** Default none. */
        flip?: 'none' | 'horizontal' | 'vertical' | 'both';
        /** Default contain. */
        fit?: 'contain' | 'cover' | 'stretch';
        /** Default h264. */
        codec?: 'h264' | 'h265';
        /** Integer 100000 - 200000000 bps. */
        bitRate?: number;
    }
    interface VideoTransformCrop {
        /** Integer 0 - 2147483647 px. Default 0. */
        x?: number;
        /** Integer 0 - 2147483647 px. Default 0. */
        y?: number;
        /** Integer 16 - 2147483647 px. */
        width: number;
        /** Integer 16 - 2147483647 px. */
        height: number;
    }

    // video.transcode(src: string, options?: VideoTranscodeOptions, output?: string | null)
    interface VideoTranscodeOptions {
        /** Integer 16 - 8192 px. */
        width?: number;
        /** Integer 16 - 8192 px. */
        height?: number;
        /** Default contain. */
        fit?: 'contain' | 'cover' | 'stretch';
        /** Default h264. */
        codec?: 'h264' | 'h265';
        /** Integer 100000 - 200000000 bps. */
        bitRate?: number;
        /** 1 - 120 fps. */
        frameRate?: number;
        audio?: VideoTranscodeAudio;
    }
    interface VideoTranscodeAudio {
        /** Integer 8000 - 512000 bps. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // video.removeAudio(src: string, output?: string | null)
    type VideoRemoveAudioOptions = Record<string, never>;

    // video.extractAudio(src: string, options?: VideoExtractAudioOptions, output?: string | null)
    interface VideoExtractAudioOptions {
        /** Default m4a. */
        format?: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // video.overlay(src: string, overlays: VideoOverlayOverlay[], output?: string | null, options?: VideoOverlayOptions)
    interface VideoOverlayOptions {
        /** Default h264. */
        codec?: 'h264' | 'h265';
        /** Integer 100000 - 200000000 bps. */
        bitRate?: number;
    }
    interface VideoOverlayTextOverlay {
        type: 'text';
        /** 1 - 1000 characters. */
        text: string;
        /** sans-serif, serif, monospace or an absolute font file path. Default sans-serif. */
        font?: string;
        /** Greater than 0, at most 1000 px. Default 48. */
        fontSize?: number;
        /** Default false. */
        bold?: boolean;
        /** Default false. */
        italic?: boolean;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. Default #FFFFFF. */
        color?: string | number;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. */
        stroke?: string | number;
        /** 0 - 100 px. */
        strokeWidth?: number;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. */
        shadow?: string | number;
        /** 0 - 100 px. Default 4. */
        shadowRadius?: number;
        /** -100 - 100 px. Default 2. */
        shadowDx?: number;
        /** -100 - 100 px. Default 2. */
        shadowDy?: number;
        /** Default center. */
        align?: 'left' | 'center' | 'right';
        /** Pixels -16384 - 16384 or a percentage "-1000%" - "1000%". Default 50%. */
        x?: number | string;
        /** Pixels -16384 - 16384 or a percentage "-1000%" - "1000%". Default 50%. */
        y?: number | string;
        /** Default center. */
        anchor?: 'center' | 'top-left' | 'top' | 'top-right' | 'left' | 'right' | 'bottom-left' | 'bottom' | 'bottom-right';
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        from?: number;
        /** Integer 1 - 9007199254740992 ms. */
        to?: number;
        /** 0 - 1. Default 1. */
        alpha?: number;
    }
    interface VideoOverlayImageOverlay {
        type: 'image';
        /** Absolute path of an image. */
        src: string;
        /** Greater than 0, at most 8. Excludes the other size keys. */
        scale?: number;
        /** Pixels or a percentage, greater than 0, at most 16384 px or 1000%. Excludes the other size keys. */
        width?: number | string;
        /** Pixels or a percentage, greater than 0, at most 16384 px or 1000%. Excludes the other size keys. */
        height?: number | string;
        /** Pixels -16384 - 16384 or a percentage "-1000%" - "1000%". Default 50%. */
        x?: number | string;
        /** Pixels -16384 - 16384 or a percentage "-1000%" - "1000%". Default 50%. */
        y?: number | string;
        /** Default center. */
        anchor?: 'center' | 'top-left' | 'top' | 'top-right' | 'left' | 'right' | 'bottom-left' | 'bottom' | 'bottom-right';
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        from?: number;
        /** Integer 1 - 9007199254740992 ms. */
        to?: number;
        /** 0 - 1. Default 1. */
        alpha?: number;
    }
    type VideoOverlayOverlay = VideoOverlayTextOverlay | VideoOverlayImageOverlay;

    // video.frames(src: string, options?: VideoFramesOptions, output?: string | null)
    interface VideoFramesOptions {
        /** 0 - 9007199254740992 ms. Excludes the other sampling keys. */
        at?: number;
        /** Up to 1000 times ms. Excludes the other sampling keys. */
        times?: number[];
        /** 1 - 9007199254740992 ms. Excludes the other sampling keys. */
        every?: number;
        /** Integer 1 - 1000. Excludes the other sampling keys. */
        count?: number;
        /** 0 - 9007199254740992 ms. Default 0. Excludes the other sampling keys. */
        start?: number;
        /** Greater than 0, at most 9007199254740992 ms. Excludes the other sampling keys. */
        end?: number;
        /** Default png. */
        format?: 'png' | 'jpg' | 'jpeg';
        /** Integer 1 - 100. Default 90. Only when format is jpeg / jpg. */
        quality?: number;
        /** Integer 16 - 8192 px. */
        width?: number;
    }

    // video.frame(src: string, at: number, options?: VideoFrameOptions)
    interface VideoFrameOptions {
        /** Default png. */
        format?: 'png' | 'jpg' | 'jpeg';
        /** Integer 1 - 100. Default 90. Only when format is jpeg / jpg. */
        quality?: number;
        /** Integer 16 - 8192 px. */
        width?: number;
    }

    // video.thumbnail(src: string, options?: VideoThumbnailOptions, output?: string | null)
    interface VideoThumbnailOptions {
        /** 0 - 9007199254740992 ms. */
        at?: number;
        /** Default jpg. */
        format?: 'png' | 'jpg' | 'jpeg';
        /** Integer 1 - 100. Default 85. Only when format is jpeg / jpg. */
        quality?: number;
        /** Integer 16 - 8192 px. */
        width?: number;
    }

    // video.toGif(src: string, options?: VideoToGifOptions, output?: string | null)
    interface VideoToGifOptions {
        /** 0 - 9007199254740992 ms. Default 0. */
        start?: number;
        /** Greater than 0, at most 9007199254740992 ms. */
        end?: number;
        /** Integer 1 - 50 fps. Default 10. */
        fps?: number;
        /** Integer 16 - 8192 px. */
        width?: number;
        /** true, false or an integer 0 - 65535. Default true. */
        loop?: boolean | number;
        /** Default false. */
        dither?: boolean;
    }

    // audio.trim(src: string, options?: AudioTrimOptions, output?: string | null)
    interface AudioTrimOptions {
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        start?: number;
        /** Integer 1 - 9007199254740992 ms. Excludes the other length keys. */
        end?: number;
        /** Integer 1 - 9007199254740992 ms. Excludes the other length keys. */
        duration?: number;
        /** Default m4a. */
        format?: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // audio.concat(sources: string[], output?: string | null, options?: AudioConcatOptions)
    interface AudioConcatOptions {
        /** Default m4a. */
        format?: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // audio.mix(inputs: (string | AudioMixInput)[], output?: string | null, options?: AudioMixOptions)
    interface AudioMixOptions {
        /** Default m4a. */
        format?: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }
    interface AudioMixInput {
        src: string;
        /** 0 - 16 x. Default 1. */
        volume?: number;
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        at?: number;
    }

    // audio.volume(src: string, options?: AudioVolumeOptions, output?: string | null)
    interface AudioVolumeOptions {
        /** 0 - 16 x. Excludes the other level keys. */
        gain?: number;
        /** true or a target -60 - 0 dBFS. Excludes the other level keys. */
        normalize?: boolean | number;
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        fadeIn?: number;
        /** Integer 0 - 9007199254740992 ms. Default 0. */
        fadeOut?: number;
        /** Default m4a. */
        format?: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // audio.convert(src: string, options: AudioConvertOptions, output?: string | null)
    interface AudioConvertOptions {
        format: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // audio.speed(src: string, options: AudioSpeedOptions, output?: string | null)
    interface AudioSpeedOptions {
        /** 0.25 - 8 x. */
        factor: number;
        /** Default true. */
        keepPitch?: boolean;
        /** Default m4a. */
        format?: 'm4a' | 'wav' | 'flac' | 'ogg';
        /** Integer 8000 - 512000 bps. Only when format is not flac / wav. */
        bitRate?: number;
        sampleRate?: 8000 | 11025 | 12000 | 16000 | 22050 | 24000 | 32000 | 44100 | 48000;
        channels?: 1 | 2;
    }

    // video.burnSubtitles(src: string, subtitles: string, style?: VideoBurnSubtitlesStyle, output?: string | null)
    type VideoBurnSubtitlesOptions = Record<string, never>;
    interface VideoBurnSubtitlesStyle {
        /** sans-serif, serif, monospace or an absolute font file path. Default sans-serif. */
        font?: string;
        /** Greater than 0, at most 1000 px. */
        fontSize?: number;
        /** Default false. */
        bold?: boolean;
        /** Default false. */
        italic?: boolean;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. Default #FFFFFF. */
        color?: string | number;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. */
        stroke?: string | number;
        /** 0 - 100 px. */
        strokeWidth?: number;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. */
        shadow?: string | number;
        /** 0 - 100 px. */
        shadowRadius?: number;
        /** -100 - 100 px. */
        shadowDx?: number;
        /** -100 - 100 px. */
        shadowDy?: number;
        /** #RGB, #RRGGBB, #AARRGGBB, a color name or an ARGB integer. */
        background?: string | number;
        /** Default bottom. */
        align?: 'center' | 'top-left' | 'top' | 'top-right' | 'left' | 'right' | 'bottom-left' | 'bottom' | 'bottom-right';
        /** Pixels 0 - 4096 or a percentage "0%" - "50%". Default 5%. */
        marginV?: number | string;
        /** Pixels 0 - 4096 or a percentage "0%" - "50%". Default 5%. */
        marginH?: number | string;
        /** Default h264. */
        codec?: 'h264' | 'h265';
        /** Integer 100000 - 200000000 bps. */
        bitRate?: number;
    }

    // subtitles.parse(path) + video.subtitles.write(src: string, output?: string | null, options: VideoSubtitlesWriteOptions)
    interface VideoSubtitlesWriteOptions {
        format: 'srt' | 'ass';
    }
}
