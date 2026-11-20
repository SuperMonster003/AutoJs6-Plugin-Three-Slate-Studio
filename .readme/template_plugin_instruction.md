{{ instruction_p_what }}

### {{ instruction_h3_start }}

{{ placeholder_instruction_start_steps }}

### {{ instruction_h3_features }}

{{ placeholder_instruction_features }}

### {{ instruction_h3_script }}

{{ instruction_p_script }}

```js
let clip = video.trim("./video.mp4", {
    start: 2000,
    duration: 5000,
    onProgress: (p) => console.log(p.stage, p.percent),
});
let gif = video.toGif(clip.path, { fps: 10, width: 480 });
console.log(gif.path, gif.sizeBytes);
```

{{ instruction_p_script_audio }}

```js
let mixed = audio.mix([
    { src: "./voice.m4a" },
    { src: "./music.m4a", volume: 0.3 },
]);
let flac = audio.convert(mixed.path, { format: "flac" });
console.log(flac.path, flac.durationMillis);
```

{{ placeholder_instruction_script_notes }}

### {{ instruction_h3_requirements }}

{{ placeholder_instruction_requirements }}

### {{ instruction_h3_faq }}

{{ placeholder_instruction_faq }}

{{ instruction_p_more }}
