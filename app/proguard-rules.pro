# Slf4j missing implementation warning
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Attributes required for reflection-based libraries (Retrofit, Gson, KSP/KAPT)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses

# Gson rules
# Keep all classes in the data model package as they are used for serialization
-keep class com.docuvio.app.data.model.** { *; }

# Keep @SerializedName fields
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep classes with @SerializedName fields (to ensure Gson can find them)
-keep @com.google.gson.annotations.SerializedName class *

# Kotlin Serialization rules
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName *;
}
# Preserve the companion object and its serializer() method for @Serializable classes
-keepclassmembers class * {
    public static ** Companion;
    public static ** serializer(...);
}

# Razorpay ProGuard rules
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**
-keepclassmembers class * {
    @com.razorpay.Retain <methods>;
}

# Retrofit rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
