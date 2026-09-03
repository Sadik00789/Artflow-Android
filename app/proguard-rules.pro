# Proguard rules for ArtFlow

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.** {
    native <methods>;
}

# MediaPipe & Tasks Vision
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**
-dontwarn javax.lang.model.**

# Preserve native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep Model definitions
-keep class com.artflow.app.model.** { *; }
-keep class com.artflow.app.engine.** { *; }
