package com.mibeko.mibeko

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Contexte applicatif injecté par Koin — même idiome que `Database.android.kt`
 * ou `PlatformUtils.android.kt`. Remplace l'ancien `MibekoApp.INSTANCE` : la
 * classe `Application` vit désormais dans le module `:androidApp`, dont ce
 * module partagé ne peut rien connaître.
 */
private object AppContextProvider : KoinComponent {
    val context: Context by inject()
}

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

// Lu sur le paquet installé (FLAG_DEBUGGABLE) plutôt que sur un `BuildConfig`
// généré par AGP : ce module n'a plus de variantes de build depuis la séparation
// d'avec l'application Android. La lecture décrit le binaire réellement exécuté
// — le pendant exact de `kotlin.native.Platform.isDebugBinary` côté iOS, et non
// une heuristique sur le nom de la tâche Gradle.
actual fun isDebugBuild(): Boolean =
    (AppContextProvider.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

actual fun getContentSharer(): com.mibeko.mibeko.util.ContentSharer =
    com.mibeko.mibeko.util.AndroidContentSharer(AppContextProvider.context)
