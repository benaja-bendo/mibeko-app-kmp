# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# ViewModels (for Koin/Voyager)
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Voyager
-keep class cafe.adriel.voyager.** { *; }

# Data Classes (Generic fallback if R8 is too aggressive with serialization)
-keepclassmembers class com.mibeko.mibeko.data.** {
    <fields>;
}
