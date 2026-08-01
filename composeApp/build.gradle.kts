import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

// Module KMP partagé (Android + iOS). La coquille applicative Android — Activity,
// Application, service FCM, manifeste, ressources, signature, R8 — vit dans
// `:androidApp` : depuis AGP 9, `com.android.application` ne peut plus cohabiter
// avec `org.jetbrains.kotlin.multiplatform` dans un même sous-projet.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.buildConfig)
}

kotlin {
    jvmToolchain(21)

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.get().compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    // Architecture mono-variante : ce module n'a ni buildTypes, ni signature, ni
    // BuildConfig généré par AGP (tout cela vit dans `:androidApp`). Les tests
    // hôte doivent être demandés explicitement — ils sont désactivés par défaut.
    android {
        namespace = "com.mibeko.mibeko.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
        withHostTestBuilder {}
    }


    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            freeCompilerArgs += "-Xbinary=bundleId=cg.mibeko.app"
        }
        // Force include KSP generated sources for iOS targets
        iosTarget.compilations.getByName("main").defaultSourceSet.kotlin.srcDir("build/generated/ksp/${iosTarget.name}/${iosTarget.name}Main/kotlin")
    }

    sourceSets {
        androidMain.dependencies {
            // `activity-compose` et `ui-tooling-preview` sont partis dans
            // `:androidApp` avec MainActivity et son @Preview.
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.androidx.security.crypto)
            implementation(libs.play.review)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.androidx.navigation)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Room
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Preferences
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.core)

            // Date & Time
            implementation(libs.kotlinx.datetime)

            
            // Markdown
            implementation(libs.markdown.renderer.m3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.multiplatform.settings.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        
        // Add KSP generated sources to the source set
        getByName("iosArm64Main").kotlin.srcDir("build/generated/ksp/iosArm64/iosArm64Main/kotlin")
        getByName("iosX64Main").kotlin.srcDir("build/generated/ksp/iosX64/iosX64Main/kotlin")
        getByName("iosSimulatorArm64Main").kotlin.srcDir("build/generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin")
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

buildConfig {
    packageName("cg.mibeko.app.common")

    // Le plugin génère du Kotlin `internal` par défaut, ce qui suffisait tant que
    // tout Android vivait dans ce module. Depuis la séparation, `MibekoApp`
    // (module `:androidApp`) lit BASE_URL : `internal` est borné au module Kotlin,
    // donc il faut le rendre public.
    useKotlinOutput { internalVisibility = false }

    // URL de production par défaut : impossible d'embarquer par accident une
    // adresse de dev dans un binaire release. Pour pointer vers un serveur
    // local en développement, ajouter dans local.properties :
    //   mibeko.dev.baseUrl=http://192.168.0.78:8000/api
    val prodBaseUrl = "https://api.mibeko.fr/api"

    val isRelease = project.gradle.startParameter.taskNames.any {
        it.contains("Release", ignoreCase = true)
    } || System.getenv("CONFIGURATION")?.equals("Release", ignoreCase = true) == true

    val devBaseUrl = rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties().apply { load(FileInputStream(file)) }
                .getProperty("mibeko.dev.baseUrl")
        }

    val baseUrl = if (isRelease || devBaseUrl.isNullOrBlank()) prodBaseUrl else devBaseUrl

    buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
}
