# =============================================================================
# Mibeko — règles R8 (release)
# =============================================================================

# --- Ktor ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- KotlinX Serialization ---
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Serializers générés pour les modèles de l'application (réseau + navigation).
-keep,includedescriptorclasses class com.mibeko.mibeko.**$$serializer { *; }
-keepclassmembers class com.mibeko.mibeko.** {
    *** Companion;
}
-keepclasseswithmembers class com.mibeko.mibeko.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Room ---
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# --- ViewModels (instanciés par Koin) ---
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# --- Modèles de données (champs lus par Room/serialization) ---
-keepclassmembers class com.mibeko.mibeko.data.** {
    <fields>;
}

# --- Navigation type-safe ---
# Les routes sont sérialisées et comparées par qualifiedName : ne pas renommer.
-keep class com.mibeko.mibeko.ui.navigation.** { *; }
