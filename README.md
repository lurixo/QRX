# QRX

[简体中文](README_zh-CN.md) | **English**

A barcode/QR code scanner and generator app with real-time scanning, batch recognition, and batch generation. Fully offline, no internet required.

## Features

### Scanning
- **Camera Scan** - Real-time scanning with flashlight support
- **Batch Recognition** - Import up to 100 images from gallery for batch scanning
- **Smart Deduplication** - Automatically filters duplicate results

| Format | Type |
|--------|------|
| EAN-13, EAN-8, UPC-A, UPC-E, Code-39, Code-93, Code-128, ITF, Codabar | 1D |
| QR Code, Data Matrix, Aztec, PDF417 | 2D |

### Generation
- **QR Code Generation** - Supports any text, URL, contact info with selectable error correction level
- **Auto Check Digit** - Automatically calculates check digits for EAN-13, EAN-8, UPC-A, UPC-E
- **Batch Generation** - Generate multiple QR codes or barcodes at once
- **Save to Gallery** - High-quality PNG format (with 50px safe margin)

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

### Others
- **Long Press Multi-select** - Batch save or delete in generation and history screens
- **Quick Settings Tile** - Quick launch scanning from control center
- **URL Detection** - Auto-detect URLs, tap to open in browser
- **History** - Categorized scan and generation history
- **Material Design 3** - Modern UI with dynamic theme colors and unified animations

## Download & Requirements

Go to [Releases](../../releases) page to download APK.

- Android 14 (API 34) or higher, arm64-v8a only
- Camera permission required for real-time scanning
- Uses Android Photo Picker for image selection, no storage permission required
- All features work fully offline

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.10 |
| KSP | 2.3.5 |
| Jetpack Compose BOM | 2026.01.01 |
| CameraX | 1.5.3 |
| ML Kit Barcode | 18.3.1 |
| Room | 2.8.4 |
| QRX-Barcode | 2.0.0 |
| Navigation Compose | 2.9.7 |
| Lifecycle | 2.10.0 |
| Activity Compose | 1.12.3 |
| Core KTX | 1.17.0 |
| Coil | 3.3.0 |
| Accompanist | 0.37.3 |
| Gradle | 9.3.1 |
| AGP | 9.0.0 |

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
