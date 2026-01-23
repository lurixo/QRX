# QRX

[简体中文](README_zh-CN.md) | **English**

A barcode/QR code scanner and generator app with real-time scanning, batch recognition, and batch generation. Fully offline, no internet required.

## Features

### Scanning
- **Camera Scan** - Real-time scanning with flashlight support
- **Barcode Support** - EAN-13, EAN-8, UPC-A, UPC-E, Code-39, Code-93, Code-128, ITF, Codabar
- **QR Code Support** - QR Code, Data Matrix, Aztec, PDF417
- **Batch Recognition** - Import up to 100 images from gallery for batch scanning
- **Smart Deduplication** - Automatically filters duplicate results

### Generation
- **QR Code Generation** - Supports any text, URL, contact info with selectable error correction level (Low for large capacity, High for print durability)
- **Barcode Generation** - CODE-128, CODE-39, CODE-93, EAN-13, EAN-8, UPC-A, UPC-E, ITF, CODABAR
- **Auto Check Digit** - Automatically calculates check digits for EAN-13, EAN-8, UPC-A, UPC-E
- **Batch Generation** - Generate multiple QR codes or barcodes at once
- **Save to Gallery** - High-quality PNG format (with 50px safe margin)

### Interaction
- **Long Press Multi-select** - Support multi-selection in generation and history screens
- **Batch Save** - Save multiple codes to gallery at once
- **Batch Delete** - Delete multiple history records at once

### Others
- **Quick Settings Tile** - Quick launch scanning from control center
- **URL Detection** - Auto-detect URLs, tap to open in browser
- **History** - Categorized scan and generation history
- **Material Design 3** - Modern UI with dynamic theme colors and unified animations

## Animation System

QRX implements a unified Material Design 3 motion system. All animations are centrally managed through the MD3 animation components, eliminating hardcoded animation values throughout the codebase.

### Animation Architecture

| Component | Description |
|-----------|-------------|
| `MD3Motion` | Central animation specs (durations, easing curves) |
| `MD3Transitions` | Page transition presets (sharedAxisX/Y, fadeThrough, containerTransform) |
| `MD3FabAnimations` | FAB show/hide animations |
| `MD3StateAnimations` | Content state change animations (empty, loading, content) |
| `MD3ListAnimations` | List item entry/exit animations |
| `MD3Components` | Reusable animated UI components |

### Animation Catalog

| Category | Animation | Specification |
|----------|-----------|---------------|
| **Navigation** | Page transition | containerTransform (scale 0.85, MEDIUM4) |
| **Navigation** | Tab switch | sharedAxisX (slide + fade, MEDIUM2) |
| **List** | Staggered entry | 30ms delay per item, max 150ms |
| **List** | Reorder | Spring (DampingRatioMediumBouncy) |
| **State** | Empty ↔ Content | fadeThrough (MEDIUM2) |
| **State** | Loading → Result | fadeThrough (SHORT4) |
| **Card** | Selection color | tween SHORT4, Standard easing |
| **Card** | Content resize | tween MEDIUM2, Emphasized easing |
| **Card** | Long press | Shape preserved with proper clipping |
| **Button** | Press scale | 96% scale, SHORT2-SHORT3 |
| **FAB** | Show/Hide | scaleIn/Out from 0.6, MEDIUM1-MEDIUM2 |
| **Dialog** | Open/Close | scaleIn from 0.85, MEDIUM4 |
| **Icon** | State toggle | Crossfade SHORT4 |
| **Snackbar** | Show/Hide | fade + slideVertically, SHORT4-MEDIUM2 |

### Duration Constants

```
SHORT1: 50ms   SHORT2: 100ms  SHORT3: 150ms  SHORT4: 200ms
MEDIUM1: 250ms MEDIUM2: 300ms MEDIUM3: 350ms MEDIUM4: 400ms
LONG1: 450ms   LONG2: 500ms   LONG3: 550ms   LONG4: 600ms
```

*Optimized to MD3 standard values (~33% faster than v4.0)*

### Easing Curves

| Curve | Usage |
|-------|-------|
| `EmphasizedDecelerate` | Entry animations (elements appearing) |
| `EmphasizedAccelerate` | Exit animations (elements disappearing) |
| `Emphasized` | State changes, content size changes (path-based interpolation) |
| `Standard` | Color transitions, property changes |
| `StandardDecelerate` | Fade in effects |
| `StandardAccelerate` | Fade out effects |

### Helper Functions

| Function | Usage |
|----------|-------|
| `standardSpec()` | Standard property transitions (SHORT4 default) |
| `emphasizedSpec()` | State/size changes (MEDIUM2 default) |
| `emphasizedDecelerateSpec()` | Entry animations (MEDIUM2 default) |
| `emphasizedAccelerateSpec()` | Exit animations (MEDIUM2 default) |
| `pressSpec(isPressed)` | Button/surface press feedback |
| `pressSpecFast(isPressed)` | Icon button press feedback |

### Refactoring Summary

All original hardcoded animations have been refactored to use the MD3 system:

| Before | After |
|--------|-------|
| `tween(300)` | `MD3Motion.emphasizedDecelerateSpec()` |
| `tween(200, easing = ...)` | `MD3Motion.standardSpec()` |
| `fadeIn()` | `fadeIn(MD3Motion.emphasizedDecelerateSpec())` |
| `scaleIn()` | `MD3FabAnimations.enter()` |
| Manual press animation | `MD3Motion.pressSpec(isPressed)` |
| Private easing constants | `MD3Motion.EmphasizedDecelerate` etc. |

## Requirements

- Android 14 (API 34) or higher
- arm64-v8a architecture
- No internet required

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.0 |
| KSP | 2.3.4 |
| Jetpack Compose BOM | 2026.01.00 |
| CameraX | 1.5.2 |
| ML Kit Barcode | 17.3.0 |
| Room | 2.8.4 |
| qrx-barcode | v1.0.1 |
| Navigation Compose | 2.9.6 |
| Lifecycle | 2.10.0 |
| Activity Compose | 1.12.2 |
| Core KTX | 1.17.0 |
| Coil | 3.3.0 |
| Accompanist | 0.37.3 |
| Gradle | 9.3.0 |
| AGP | 9.0.0 |

## Supported Formats

### Generation

| Format | Type | Length | Characters |
|--------|------|--------|------------|
| QR Code | 2D | Unlimited | Any |
| CODE-128 | 1D | Unlimited | ASCII |
| CODE-39 | 1D | Unlimited | A-Z, 0-9, -. $/+% |
| CODE-93 | 1D | Unlimited | A-Z, 0-9, -. $/+% |
| EAN-13 | 1D | 13 digits | 0-9 |
| EAN-8 | 1D | 8 digits | 0-9 |
| UPC-A | 1D | 12 digits | 0-9 |
| UPC-E | 1D | 8 digits | 0-9 |
| ITF | 1D | Even digits | 0-9 |
| CODABAR | 1D | Unlimited | 0-9, -$:/.+, A-D |

### Scanning (additional formats)
- Data Matrix
- Aztec
- PDF417

## Download

Go to [Releases](../../releases) page to download APK.

## Permissions

| Permission | Usage |
|------------|-------|
| `CAMERA` | Real-time barcode/QR code scanning |

This app uses Android Photo Picker for image selection, no storage permission required. All features work offline.

## Project Structure

```
app/src/main/java/io/qrx/scan/
├── MainActivity.kt
├── ScanActivity.kt
├── QRXApplication.kt
├── QRXTileService.kt
├── data/
│   ├── AppDatabase.kt
│   ├── ScanHistoryDao.kt
│   ├── ScanHistoryEntity.kt
│   ├── GenerateHistoryDao.kt
│   └── GenerateHistoryEntity.kt
├── ui/
│   ├── screens/
│   │   ├── ScanScreen.kt
│   │   ├── CameraScanScreen.kt
│   │   ├── HistoryScreen.kt
│   │   ├── QRCodeGenerateScreen.kt
│   │   └── BarcodeGenerateScreen.kt
│   ├── components/
│   │   ├── ScanResultCard.kt
│   │   ├── MD3Components.kt
│   │   └── QRXSnackbar.kt
│   ├── animation/
│   │   └── MD3Motion.kt
│   └── theme/
│       └── Theme.kt
└── util/
    ├── BarcodeGenerator.kt
    └── CrashHandler.kt
```

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
