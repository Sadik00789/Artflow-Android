# ArtFlow — Offline On-Device Neural Art Studio

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2034)-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.02-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow%20Lite-2.14.0%20(FP16)-FF6F00.svg?style=flat&logo=tensorflow)](https://www.tensorflow.org/lite)
[![Inference](https://img.shields.io/badge/Inference-100%25%20On--Device%20Offline-green.svg?style=flat)](#strict-offline--privacy-first)
[![Hardware Target](https://img.shields.io/badge/Baseline-Snapdragon%20695%20(Adreno%20619)-blue.svg?style=flat)](#hardware-target--device-baseline)

**ArtFlow** is a production-grade, 100% offline, on-device neural art studio for Android. It transforms ordinary photos into fine art paintings, anime drawings, and graphic illustrations using hardware-accelerated deep neural networks running directly on the device's GPU—with **zero cloud dependencies, zero telemetry, and zero network calls**.

---

## Key Highlights

- **50 Neural Art Styles**: Curated collection spanning **Fine Art** (18 styles), **Anime** (16 styles), and **Graphic Design** (16 styles).
- **Hero Neural Architectures**:
  - **Johnson et al. Fast-Neural-Style (`TransformerNet`)** with residual blocks and instance normalization.
  - **Official AnimeGANv2 (`AnimeGANGenerator`)** with inverted residual blocks, depthwise convolutions, and bilateral upsamplers.
- **Studio HD Export Pipeline**: 3-stage post-processing pipeline upscaling to $1536\text{px}$ using **FSRCNN 2x Super-Resolution**, high-pass luminance detail injection ($12\%$), and edge-aware thresholded unsharp masking ($|\Delta| > 8$).
- **Selfie Segmentation & Subject Protection**: Real-time on-device portrait isolation with $2\text{px}$ morphological erosion and $7\text{px}$ box-blur edge feathering.
- **Snapdragon 695 / Adreno 619 Optimization**:
  - **OpenCL FP16 GPU Delegate** with sustained performance preferences and automatic 4-thread CPU XNNPACK fallback.
  - **2-Slot LRU Cache (`ModelLruCache`)** with background asynchronous disposal to prevent GPU context exhaustion.
  - **Zero-Copy Memory Mapping (`mmap`)**: Models stored uncompressed (`stored`) in the APK for immediate `FileChannel` address mapping without heap copies.
  - **Dynamic Spatial Reshaping**: Inference supports arbitrary portrait and landscape aspect ratios up to the $768\text{px}$ baseline.
- **Modern Jetpack Compose UI**: Edge-to-edge Material 3 dark theme, 120Hz smooth scrolling carousel, interactive pinch-to-zoom/pan canvas with crossfade transitions, dual intensity and subject isolation sliders, and a multi-stage export HUD.

---

## Architectural Workflow

```mermaid
flowchart TD
    subgraph InputStage ["1. WYSIWYG Canvas Input"]
        A[Camera / Gallery Image] --> B[ImageNormalizer: Max 768px Even Dims]
    end

    subgraph StudioEngine ["2. Interactive Studio Engine (220-320ms on Adreno 619)"]
        B --> C[ModelLruCache: 2-Slot GPU LRU]
        C --> D[GpuDelegateProvider: OpenCL FP16 / 4-Thread XNNPACK]
        D --> E[StyleTransferEngine: Dynamic Reshaping]
        B --> F[PortraitSegmenter: Selfie Segmentation]
        F --> G[MaskProcessor: 2px Erosion + 7px Box Blur]
        E --> H[Compositor: Dual-Slider Alpha Blending]
        G --> H
        H --> I[ViewportCanvas: CrossfadeLayer & Pinch-Zoom]
    end

    subgraph ExportStage ["3. Multi-Stage Studio HD Export (1536px)"]
        H --> J[Stage 1: FSRCNN 2x Neural Upscaler]
        A --> K[Original Image High Frequencies]
        J --> L[Stage 2: YCbCr 12% Luminance Detail Injection]
        K --> L
        L --> M[Stage 3: Edge-Aware Thresholded Unsharp Mask |Δ|>8]
        M --> N[MediaStoreWriter: Gallery Save + EXIF Metadata]
    end
```

---

## Model Catalog

ArtFlow bundles **52 optimized neural network models** directly in the APK:

### 1. Fine Art Presets (18 Models)
Based on Johnson et al. Fast-Neural-Style `TransformerNet` architecture (~3.3 MB each):
- `starry_night` — Vincent van Gogh
- `the_scream` — Edvard Munch
- `great_wave` — Hokusai
- `guernica` — Pablo Picasso
- `monet_water_lilies` — Claude Monet
- `kandinsky_composition` — Wassily Kandinsky
- `klimt_the_kiss` — Gustav Klimt
- `van_gogh_sunflowers` — Vincent van Gogh
- `cezanne_mont_sainte_victoire` — Paul Cézanne
- `turner_rain_steam_speed` — J.M.W. Turner
- `hokusai_red_fuji` — Hokusai
- `degas_ballet` — Edgar Degas
- `renoir_boating_party` — Pierre-Auguste Renoir
- `munch_madonna` — Edvard Munch
- `picasso_weeping_woman` — Pablo Picasso
- `matisse_dance` — Henri Matisse
- `gauguin_tahitian_women` — Paul Gauguin
- `seurat_la_grande_jatte` — Georges Seurat

### 2. Anime Presets (16 Models)
Based on Bryan D. Lee / TachibanaYoshino AnimeGANv2 `AnimeGANGenerator` architecture (~4.2 MB each):
- `shinkai_sky` — Makoto Shinkai aesthetic
- `ghibli_pastoral` — Studio Ghibli lush greens and soft tones
- `cyberpunk_neo_tokyo` — High-contrast neon anime look
- `retro_80s_anime` — Hand-cel aesthetic of classic 80s OVA
- `kyoto_bloom` — Vibrant pastel floral tones
- `manga_ink_wash` — High-contrast screentone and ink wash
- `ufotable_digital` — Modern composited digital lighting
- `chibi_pastel` — Soft cartoon palette
- `mecha_cel_shade` — Crisp mechanical edges and metallic sheen
- `lofi_chill` — Muted nostalgic tones
- `fantasy_isekai` — Luminous fantasy scenery
- `shoujo_sparkle` — Romantic watercolor aesthetic
- `dark_fantasy_berserk` — Grim heavy shadows
- `vaporwave_sunset` — 80s anime synth palette
- `city_pop_1984` — Tokyo retro-pop styling
- `trigger_action` — Dynamic saturated action styling

### 3. Graphic & Vision Models (18 Models)
- **16 Graphic Styles**: `bauhaus_geometry`, `pop_art_warhol`, `risograph_print`, `synthwave_neon`, `comic_halftone`, `swiss_typographic`, `art_deco_gold`, `cyber_glitch`, `blueprint_cyanotype`, `vector_flat`, `stencil_street_art`, `linocut_print`, `psychedelic_60s`, `holographic_iridescent`, `charcoal_sketch`, `woodblock_ukiyoe`.
- **2 Vision Models**:
  - `vision/fsrcnn_x2_fp16.tflite` — 2x neural super-resolution upscaler.
  - `vision/selfie_segmenter.tflite` — On-device portrait segmenter.

---

## Studio HD Export Pipeline

Exporting an artwork runs through an edge-aware 3-stage enhancement pipeline:

1. **Stage 1: FSRCNN 2x Super-Resolution**
   - The interactive $768\text{px}$ canvas is upscaled $2\times$ to $1536\text{px}$ via neural transposed convolution without bilinear blur.
2. **Stage 2: YCbCr Luminance Detail Injection**
   - Extracts high-frequency micro-details from the full-resolution original image.
   - Injects $12\%$ of the original high-pass luminance detail into the $Y$ channel of the upscaled artwork, preserving authentic skin pores, eyelashes, and textural boundaries.
3. **Stage 3: Edge-Aware Thresholded Unsharp Mask**
   - Computes a Gaussian unsharp mask on the luminance channel.
   - Applies sharpening only where luminance delta satisfies $|\Delta| > 8$ to avoid amplifying flat-color noise.
   - Clamps correction magnitude to $\pm 12$ intensity units to eliminate ringing halos.
4. **MediaStore Export with EXIF Tags**
   - Writes directly to Android `Pictures/ArtFlow` with injected EXIF metadata (`Artist: ArtFlow On-Device Studio`, `ImageDescription: Style Preset Name`).

---

## Project Structure

```text
artflow-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/models/             # 52 FP16 TFLite models
│   │   │   │   ├── fine_art/              # 18 Fast-Neural-Style models
│   │   │   │   ├── anime/                 # 16 AnimeGANv2 models
│   │   │   │   ├── graphic/               # 16 Graphic models
│   │   │   │   └── vision/                # FSRCNN x2 & Selfie Segmenter
│   │   │   ├── java/com/artflow/app/
│   │   │   │   ├── core/
│   │   │   │   │   ├── common/            # Result, DispatcherProvider
│   │   │   │   │   └── storage/           # AssetModelReader, MediaStoreWriter
│   │   │   │   ├── engine/                # Inference Engine
│   │   │   │   │   ├── GpuDelegateProvider.kt   # OpenCL FP16 + CPU Fallback
│   │   │   │   │   ├── ModelLruCache.kt         # 2-Slot GPU LRU Cache
│   │   │   │   │   ├── DynamicTensorHandler.kt  # Dynamic Reshaping
│   │   │   │   │   ├── StyleTransferEngine.kt   # Core Inference Engine
│   │   │   │   │   ├── export/            # 3-Stage Studio HD Export Pipeline
│   │   │   │   │   ├── processing/        # ImageNormalizer, Compositor
│   │   │   │   │   └── segmentation/      # PortraitSegmenter, MaskProcessor
│   │   │   │   ├── model/                 # StyleCatalog (50 styles), Presets
│   │   │   │   └── ui/                    # Jetpack Compose UI
│   │   │   │       ├── editor/            # ViewModel, Canvas, Carousel, Sliders
│   │   │   │       ├── theme/             # Material 3 Dark Palette
│   │   │   │       └── MainActivity.kt    # PhotoPicker, Permissions, Edge-to-Edge
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                          # JVM Unit Tests (Robolectric/JUnit)
│   │   └── androidTest/                   # On-Device Adreno GPU Benchmarks
│   └── build.gradle.kts
├── tools/                                 # Offline Python ML Pipeline
│   ├── checkpoints/                       # Raw PyTorch .pth & .pt weights
│   ├── models/
│   │   ├── transformer_net.py             # Johnson et al. TransformerNet
│   │   └── animegan_generator.py          # Official AnimeGANv2 Generator
│   ├── converters/
│   │   └── builder.py                     # TF Modules & FP16 Exporters
│   ├── wrapper/
│   │   ├── normalizer.py                  # UnifiedStyleWrapper Graph Contract
│   │   └── dynamic_onnx.py                # ONNX Dynamic Spatial Exporter
│   ├── download_hero_checkpoints.py       # Automated Weight Downloader
│   ├── convert_all.py                     # 52-Model Batch Converter
│   └── verify_tflite.py                   # Automated Numerical & NaN Verifier
├── gradle/libs.versions.toml              # Version Catalog
├── build.gradle.kts                       # Root Gradle Script
└── settings.gradle.kts
```

---

## Environment & Build Prerequisites

- **JDK**: Eclipse Temurin or OpenJDK 17 (`JAVA_HOME` pointing to JDK 17).
- **Android SDK**: Build-Tools `35.0.0` or `34.0.0`, Platform API `35`, Platform-Tools.
- **Gradle**: 8.10.2 (bundled wrapper `gradlew`).
- **Python (Optional for Model Toolchain)**: Python 3.10 or 3.11 with PyTorch 2.x and TensorFlow 2.x.

---

## Quick Start & Verification

### 1. Build the Android Debug APK
```bash
./gradlew assembleDebug
```
The resulting debug APK is output to:
```text
app/build/outputs/apk/debug/app-debug.apk
```
*Current packaged APK size is ~163 MB, bundling all 52 FP16 neural models uncompressed for direct memory mapping.*

### 2. Run JVM Unit Tests
Execute the local unit test suite covering image dimension snapping, 2-slot LRU cache eviction, and ViewModel coroutine cancellation:
```bash
./gradlew testDebugUnitTest
```

### 3. (Optional) Re-run Python ML Model Conversion
If you want to re-download hero checkpoints and re-generate the 52 TFLite assets:
```bash
# 1. Download official PyTorch and AnimeGANv2 checkpoints
python3 tools/download_hero_checkpoints.py

# 2. Batch convert all 52 models to FP16 TFLite
python3 tools/convert_all.py

# 3. Verify numerical bounds, shapes, and zero NaNs
python3 tools/verify_tflite.py
```

---

## Hardware Target & Device Baseline

ArtFlow is engineered specifically for mid-range mobile silicon:
- **Baseline SoC**: Qualcomm Snapdragon 695 5G (SM6375).
- **GPU**: Qualcomm Adreno 619.
- **Target Frame Latency**: $220\text{ms} - 320\text{ms}$ interactive preview on $768\text{px}$ canvas.
- **Memory Overhead**: Peak RAM $< 380\text{MB}$ during interactive studio session, $< 650\text{MB}$ during $1536\text{px}$ multi-stage HD export.

---

## Strict Offline & Privacy First

- **0 Remote APIs**: ArtFlow contains zero network client libraries and makes zero HTTP requests.
- **0 Telemetry / Analytics**: No tracking SDKs (no Firebase Analytics, no crash reporters, no tracking IDs).
- **Local Storage Only**: Photos selected via the system Photo Picker remain in app memory or are saved directly to local `MediaStore`.

---

## Attributions & Open-Source Credits

- **AnimeGANv2**: PyTorch port by [Bryan D. Lee](https://github.com/bryandlee/animegan2-pytorch) and original architecture by [TachibanaYoshino](https://github.com/TachibanaYoshino/AnimeGANv2).
- **Fast-Neural-Style**: Architecture and pretrained weights by [Justin Johnson et al.](https://github.com/pytorch/examples/tree/main/fast_neural_style).
- **FSRCNN**: Fast Super-Resolution Convolutional Neural Network by Dong et al.
- **MediaPipe**: Selfie Segmentation by Google.

---

## License

This project is licensed under the Apache License, Version 2.0. Model checkpoints are subject to their respective open-source licenses (MIT / Apache 2.0).
