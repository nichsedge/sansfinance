# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.RoomDatabase

# Hilt
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# LiteRT / TensorFlow Lite
-keep class com.google.ai.edge.litertlm.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses, Signature
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,allowobfuscation,allowshrinking class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    public static ** Companion;
}
-keepclassmembers class * {
    public static ** serializer(...);
}

# Keep domain models that might be serialized/deserialized
-keep class com.sans.finance.domain.model.** { *; }
-keep class com.sans.finance.data.local.entity.** { *; }
