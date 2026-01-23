# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Coil
-dontwarn coil.**

# Room entities
-keep class io.qrx.scan.data.** { *; }

# qrx-barcode (ZXing-based)
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
