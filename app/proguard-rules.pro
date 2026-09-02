# Proguard rules for ArtFlow

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.gms.tflite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.** {
    native <methods>;
}

# Preserve native JNI methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Compose runtime
-keep class androidx.compose.** { *; }

# Keep Model definitions
-keep class com.artflow.app.model.** { *; }
-keep class com.artflow.app.engine.** { *; }
