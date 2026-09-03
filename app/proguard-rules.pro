# Proguard rules for ArtFlow

# Disable class/method/field obfuscation to guarantee 100% reflection & JNI compatibility
-dontobfuscate

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.** {
    native <methods>;
}

# MediaPipe & Tasks Vision & ODML & Protobuf & Flogger
-keep class com.google.mediapipe.** { *; }
-keep class com.google.android.odml.** { *; }
-keep class com.google.protobuf.** { *; }
-keep class com.google.common.flogger.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.android.odml.**
-dontwarn com.google.protobuf.**
-dontwarn com.google.common.flogger.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**
-dontwarn javax.lang.model.**

# Preserve native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep Model and UI state definitions
-keep class com.artflow.app.model.** { *; }
-keep class com.artflow.app.engine.** { *; }
-keep class com.artflow.app.ui.editor.EditorUiState** { *; }


