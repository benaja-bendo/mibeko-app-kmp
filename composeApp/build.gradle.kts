import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.room)
    alias(libs.plugins.buildConfig)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseAppDistribution)
    alias(libs.plugins.firebaseCrashlytics)
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

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
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
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)
            implementation(libs.androidx.security.crypto)
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
        }
        
        // Add KSP generated sources to the source set
        getByName("iosArm64Main").kotlin.srcDir("build/generated/ksp/iosArm64/iosArm64Main/kotlin")
        getByName("iosX64Main").kotlin.srcDir("build/generated/ksp/iosX64/iosX64Main/kotlin")
        getByName("iosSimulatorArm64Main").kotlin.srcDir("build/generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin")
    }
}

// Les secrets de signature vivent HORS du dépôt (voir audit 2026-07, P0.10).
// Chemin surchargeable : -PkeystorePropertiesFile=/chemin/keystore.properties
// ou variable d'environnement MIBEKO_KEYSTORE_PROPERTIES.
// Défaut : ../../secrets/mibeko-app-kmp/keystore.properties (relatif à la racine
// du projet). Absent => pas de signature locale (le build debug fonctionne,
// la CI passe par les variables KEYSTORE_FILE/KEYSTORE_PASSWORD/...).
val keystorePropertiesFile: File = (
    (project.findProperty("keystorePropertiesFile") as String?)
        ?: System.getenv("MIBEKO_KEYSTORE_PROPERTIES")
    )?.let { rootProject.file(it) }
    ?: rootProject.file("../../secrets/mibeko-app-kmp/keystore.properties")

android {
    namespace = "com.mibeko.mibeko"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cg.mibeko.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Surchargables par la CI : ./gradlew bundleRelease -PversionCode=12 -PversionName=1.2.0
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 2
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
    }
    
    buildFeatures {
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                // Chemin du .jks résolu depuis l'emplacement du fichier de
                // propriétés (le keystore vit à côté, hors du dépôt).
                storeFile = keystorePropertiesFile.parentFile
                    .resolve(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else if (System.getenv("KEYSTORE_FILE") != null) {
                storeFile = file(System.getenv("KEYSTORE_FILE")!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // BASE_URL is now provided by gmazzo buildConfig plugin

            // Crashlytics : en debug on n'upload PAS le fichier de mapping
            // (obfuscation désactivée de toute façon) → build debug rapide et
            // sans dépendance à une config release absente. Le report d'exception
            // à l'exécution (recordException) reste actif.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            
            // On s'assure que la configuration de signature est utilisée si elle a été configurée
            if (keystorePropertiesFile.exists() || System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }

            // Configuration Firebase App Distribution
            configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
                artifactType = "APK"
                
                // Utilise la propriété passée par la CI ou la version courante
                val ciReleaseNotes = project.findProperty("appDistribution-releaseNotes") as? String
                val currentVersion = defaultConfig.versionName
                releaseNotes = ciReleaseNotes ?: "Version $currentVersion"
                
                groups = "utilisateur-lambda"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
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
