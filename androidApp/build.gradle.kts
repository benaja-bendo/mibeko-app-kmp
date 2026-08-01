import java.io.FileInputStream
import java.util.Properties

// Module application Android : coquille d'entrée (Activity, Application, service
// FCM, manifeste, ressources, signature, R8) au-dessus du module KMP `:composeApp`,
// qui porte tout le code partagé. Cette séparation est imposée par AGP 9 : le
// plugin `com.android.application` et `org.jetbrains.kotlin.multiplatform` ne
// peuvent plus cohabiter dans un même sous-projet.
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseAppDistribution)
    alias(libs.plugins.firebaseCrashlytics)
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
        // En debug, un -PversionCode manquant retombe sur 2 (jamais publié, sans
        // conséquence). En assemble/bundle release, il est obligatoire : le repli
        // silencieux sur 2 a provoqué un rejet Play Console le 01/08/2026 (release
        // "internal" refusée car versionCode déjà utilisé par un build CI antérieur).
        val isReleasePackagingTask = project.gradle.startParameter.taskNames.any { taskName ->
            val name = taskName.substringAfterLast(":")
            name.contains("Release") && (name.startsWith("assemble") || name.startsWith("bundle") || name.startsWith("package"))
        }
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull()
            ?: if (isReleasePackagingTask) {
                throw GradleException(
                    "versionCode manquant pour un build release. Passe -PversionCode=<n> " +
                        "(la CI le calcule via `git rev-list --count HEAD`, voir .github/workflows/release-play.yml)."
                )
            } else {
                2
            }
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
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
    implementation(projects.composeApp)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Koin : `MibekoApp` démarre le conteneur, `MyFirebaseMessagingService` y puise
    // ses dépendances (KoinComponent).
    implementation(libs.koin.core)
    implementation(libs.koin.android)

    // Firebase : seul Messaging est utilisé ici (service FCM). Analytics et
    // Crashlytics sont consommés depuis `:composeApp`.
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
