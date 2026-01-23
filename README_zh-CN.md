# QRX

**简体中文** | [English](README.md)

条码/二维码扫描与生成工具，支持实时扫描、批量识别和批量生成。纯离线应用，无需联网。

## 功能特性

### 扫描识别
- **扫一扫** - 相机实时扫描，支持闪光灯
- **条形码支持** - EAN-13、EAN-8、UPC-A、UPC-E、Code-39、Code-93、Code-128、ITF、Codabar
- **二维码支持** - QR Code、Data Matrix、Aztec、PDF417
- **批量识别** - 从相册导入多张图片批量识别（最多100张）
- **智能去重** - 自动过滤重复的识别内容

### 生成功能
- **二维码生成** - 支持任意文本、网址、联系方式，可选纠错级别（Low 容量大，High 纠错强）
- **条形码生成** - CODE-128、CODE-39、CODE-93、EAN-13、EAN-8、UPC-A、UPC-E、ITF、CODABAR
- **自动校验位** - EAN-13、EAN-8、UPC-A、UPC-E 自动计算校验位
- **批量生成** - 一次生成多个二维码或条形码
- **保存到相册** - 高清 PNG 格式（含50px安全边距）

### 交互功能
- **长按多选** - 生成界面和历史界面支持长按多选
- **批量保存** - 选中多个码后批量保存到相册
- **批量删除** - 选中多个历史记录后批量删除

### 其他功能
- **控制中心磁贴** - 下拉快捷启动扫描
- **链接识别** - 自动识别 URL，点击跳转浏览器
- **历史记录** - 分类保存扫描记录和生成记录
- **Material Design 3** - 现代化界面，动态主题色，统一动画

## 动画系统

QRX 实现了统一的 Material Design 3 动效系统。所有动画通过 MD3 动画组件集中管理，消除了代码中的硬编码动画值。

### 动画架构

| 组件 | 说明 |
|------|------|
| `MD3Motion` | 动画规范中心（时长、缓动曲线） |
| `MD3Transitions` | 页面切换预设（sharedAxisX/Y、fadeThrough、containerTransform） |
| `MD3FabAnimations` | FAB 显示/隐藏动画 |
| `MD3StateAnimations` | 内容状态切换动画（空态、加载、内容） |
| `MD3ListAnimations` | 列表项进入/退出动画 |
| `MD3Components` | 可复用的动画 UI 组件 |

### 动画清单

| 类别 | 动画 | 规范 |
|------|------|------|
| **导航** | 页面切换 | containerTransform（缩放 0.85, MEDIUM4） |
| **导航** | 标签切换 | sharedAxisX（滑动 + 淡入淡出, MEDIUM2） |
| **列表** | 渐进式进场 | 每项延迟 30ms，最大 150ms |
| **列表** | 重排序 | 弹性动画（DampingRatioMediumBouncy） |
| **状态** | 空态 ↔ 内容态 | fadeThrough（MEDIUM2） |
| **状态** | 加载 → 结果 | fadeThrough（SHORT4） |
| **卡片** | 选中颜色 | tween SHORT4, Standard 缓动 |
| **卡片** | 内容尺寸 | tween MEDIUM2, Emphasized 缓动 |
| **卡片** | 长按交互 | 正确的圆角裁剪保持 |
| **按钮** | 按压缩放 | 96% 缩放，SHORT2-SHORT3 |
| **FAB** | 显示/隐藏 | scaleIn/Out 从 0.6，MEDIUM1-MEDIUM2 |
| **对话框** | 打开/关闭 | scaleIn 从 0.85，MEDIUM4 |
| **图标** | 状态切换 | Crossfade SHORT4 |
| **Snackbar** | 显示/隐藏 | fade + slideVertically，SHORT4-MEDIUM2 |

### 时长常量

```
SHORT1: 50ms   SHORT2: 100ms  SHORT3: 150ms  SHORT4: 200ms
MEDIUM1: 250ms MEDIUM2: 300ms MEDIUM3: 350ms MEDIUM4: 400ms
LONG1: 450ms   LONG2: 500ms   LONG3: 550ms   LONG4: 600ms
```

*已优化为 MD3 标准值（比 v4.0 快约 33%）*

### 缓动曲线

| 曲线 | 用途 |
|------|------|
| `EmphasizedDecelerate` | 进入动画（元素出现） |
| `EmphasizedAccelerate` | 退出动画（元素消失） |
| `Emphasized` | 状态变化、内容尺寸变化（路径插值实现） |
| `Standard` | 颜色过渡、属性变化 |
| `StandardDecelerate` | 淡入效果 |
| `StandardAccelerate` | 淡出效果 |

### 辅助函数

| 函数 | 用途 |
|------|------|
| `standardSpec()` | 标准属性过渡（默认 SHORT4） |
| `emphasizedSpec()` | 状态/尺寸变化（默认 MEDIUM2） |
| `emphasizedDecelerateSpec()` | 进入动画（默认 MEDIUM2） |
| `emphasizedAccelerateSpec()` | 退出动画（默认 MEDIUM2） |
| `pressSpec(isPressed)` | 按钮/表面按压反馈 |
| `pressSpecFast(isPressed)` | 图标按钮按压反馈 |

### 重构总结

所有原始硬编码动画已重构为 MD3 系统：

| 重构前 | 重构后 |
|--------|--------|
| `tween(300)` | `MD3Motion.emphasizedDecelerateSpec()` |
| `tween(200, easing = ...)` | `MD3Motion.standardSpec()` |
| `fadeIn()` | `fadeIn(MD3Motion.emphasizedDecelerateSpec())` |
| `scaleIn()` | `MD3FabAnimations.enter()` |
| 手动按压动画 | `MD3Motion.pressSpec(isPressed)` |
| 私有缓动常量 | `MD3Motion.EmphasizedDecelerate` 等 |

## 系统要求

- Android 14 (API 34) 及以上
- arm64-v8a 架构
- 无需联网

## 技术栈

| 组件 | 版本 |
|------|------|
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

## 支持的格式

### 生成

| 格式 | 类型 | 长度 | 支持字符 |
|------|------|------|----------|
| QR Code | 二维码 | 无限制 | 任意字符 |
| CODE-128 | 一维码 | 无限制 | ASCII字符 |
| CODE-39 | 一维码 | 无限制 | A-Z, 0-9, -. $/+% |
| CODE-93 | 一维码 | 无限制 | A-Z, 0-9, -. $/+% |
| EAN-13 | 一维码 | 13位 | 0-9 |
| EAN-8 | 一维码 | 8位 | 0-9 |
| UPC-A | 一维码 | 12位 | 0-9 |
| UPC-E | 一维码 | 8位 | 0-9 |
| ITF | 一维码 | 偶数位 | 0-9 |
| CODABAR | 一维码 | 无限制 | 0-9, -$:/.+, A-D |

### 扫描（额外支持）
- Data Matrix
- Aztec
- PDF417

## 下载

前往 [Releases](../../releases) 页面下载 APK。

## 权限说明

| 权限 | 用途 |
|------|------|
| `CAMERA` | 实时扫描二维码/条形码 |

本应用使用 Android 照片选择器选取图片，无需存储权限。所有功能均离线运行。

## 项目结构

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

## 许可证

本项目采用 GNU 通用公共许可证 v3.0 - 详情请参阅 [LICENSE](LICENSE) 文件。
