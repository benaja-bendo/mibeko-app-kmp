# Migration AGP 10 — séparation KMP / application Android

> Statut : à jour au 1er août 2026 · **Fait autorité sur** : l'état des lieux des dépréciations AGP 9 du dépôt `mibeko-app-kmp` et le plan de migration vers la structure en sous-projets. Plan en cours — ce document meurt une fois la migration exécutée.

## 1. Ce que dit la CI, et ce qu'elle ne dit pas

Trois réglages de `gradle.properties` sont des béquilles supprimées en AGP 10 :

| Ligne | Réglage | Sans lui, aujourd'hui |
| --- | --- | --- |
| 33 | `android.newDsl=false` | `Failed to apply plugin 'org.jetbrains.kotlin.multiplatform'` — `androidTarget()` incompatible avec le nouveau DSL |
| 32 | `android.builtInKotlin=false` | `Failed to apply plugin 'com.android.internal.application'` — AGP refuse `com.android.application` + KMP dans le même module |
| — | (aucun autre) | les 4 autres `android.*` de `gradle.properties` (lignes 27-31) ne sont pas dépréciés |

Reproduction :

```bash
./gradlew :composeApp:assembleDebug --dry-run -Pandroid.newDsl=true --no-configuration-cache
```

## 2. Qui appelle l'API variant obsolète — mesuré, pas supposé

Commande :

```bash
./gradlew :composeApp:assembleDebug --dry-run -Pandroid.debug.obsoleteApi=true --no-configuration-cache
```

Trois API obsolètes sont signalées — `applicationVariants`, `testVariants`, `unitTestVariants` — et les trois traces convergent vers **un seul appelant** :

```
com.android.build.gradle.AbstractAppExtension.getApplicationVariants
org.jetbrains.kotlin.gradle.utils.ForEachAndroidVariantKt.forAllAndroidVariants   (forEachAndroidVariant.kt:21 / 29 / 30)
org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSets.applyKotlinAndroidSourceSetLayout
org.jetbrains.kotlin.gradle.plugin.AndroidProjectHandler.configureTarget
org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTargetPreset.createTargetInternal
    → composeApp/build.gradle.kts:31   androidTarget { }
```

**L'appelant est le plugin Kotlin Multiplatform lui-même**, pas `google-services` ni `firebase-crashlytics`. Conséquences :

- **aucune montée de version de plugin ne résout le problème** — ni celle de KGP : l'appel est structurel, KGP est obligé de passer par l'API legacy tant que `androidTarget()` cohabite avec `com.android.application` ;
- les deux familles d'avertissements (§1 et §2) sont **le même problème avec le même correctif** ; il n'y a pas deux chantiers ;
- `google-services`, `firebase-crashlytics`, `firebase-appdistribution`, `room`, `ksp`, `buildConfig`, `compose` : **zéro appel** à l'API obsolète.

Ce dernier point a été vérifié positivement sur banc d'essai (§3), pas seulement par absence dans les traces.

## 3. Banc d'essai : la cible compile aux versions actuelles

Projet jetable monté hors dépôt, aux versions **exactes** du projet (AGP 9.2.1, Kotlin 2.3.20, Gradle 9.4.1, Compose MP 1.10.3, Room 2.8.4, KSP 2.3.6), **sans** `android.newDsl=false` ni `android.builtInKotlin=false` :

- `:shared` — `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` + Compose MP + Room/KSP + les 3 cibles iOS ;
- `:app` — `com.android.application` + `google-services` 4.4.4 + `firebase-crashlytics` 3.0.6 + `firebase-appdistribution` 5.2.1, dépendant de `:shared`.

Résultats :

| Vérification | Résultat |
| --- | --- |
| `:app:assembleDebug` | ✅ |
| `:app:assembleRelease` (R8 + 3 plugins Firebase) | ✅ |
| `:shared:linkDebugFrameworkIosArm64` | ✅ |
| Avertissements de dépréciation AGP | **aucun** |
| Configurations `kspAndroid` / `kspIos*` | conservées (tâches `kspAndroidMain`, `kspKotlinIosArm64`) |

La cible est donc atteignable **sans monter aucune version**. Le bloc de configuration s'écrit `kotlin { android { … } }` (`androidLibrary { }` fonctionne aussi mais correspond à l'interface `Deprecated…` d'AGP).

## 4. Ce que le nouveau plugin ne sait pas faire

`KotlinMultiplatformAndroidLibraryExtension` (AGP 9.2.1, vérifié par introspection du jar) expose `namespace`, `compileSdk`, `minSdk`, `androidResources`, `packaging`, `optimization`, `lint`, `withHostTest`, `withDeviceTest`. Il **n'expose pas** `defaultConfig`, `buildTypes`, `signingConfigs`, `compileOptions`, ni `buildFeatures.buildConfig` : architecture mono-variante, pas de `BuildConfig`.

D'où les points de contact réels avec le code existant :

| Point | Fichier | Traitement |
| --- | --- | --- |
| `BuildConfig.DEBUG` (généré par AGP) | `Platform.android.kt:13` | passer par le plugin `buildConfig` gmazzo, déjà en place et déjà commun aux 2 OS |
| `R.string` / `R.mipmap` | `MyFirebaseMessagingService.kt:88,91` | le fichier part dans `:androidApp` (il importe déjà `MainActivity`) |
| `MibekoApp.INSTANCE` | `Platform.android.kt:16`, `AnalyticsManager.android.kt:12`, `SecureSettings.android.kt:22` | résoudre le `Context` par Koin, comme le font **déjà** `Database.android.kt`, `PlatformUtils.android.kt`, `NetworkConnectivityChecker.android.kt` et `NotificationManager.android.kt` |
| `import MainActivity` | `NotificationManager.android.kt:11` | intent implicite (`getLaunchIntentForPackage`) |
| `res/xml/network_security_config.xml` variante debug | `src/androidDebug/` | part dans `:androidApp`, qui garde les `buildTypes` |
| `debugImplementation(compose.ui.tooling)` | `build.gradle.kts` | part dans `:androidApp` |

Le gros du travail est donc **déjà fait** : 4 des 7 fichiers `androidMain` qui ont besoin d'un `Context` passent par Koin. Il reste 3 usages de `MibekoApp.INSTANCE` et 2 imports de `MainActivity`.

## 5. Chemins à réécrire hors Gradle

Noms de tâches, relevés sur le banc d'essai :

| Aujourd'hui | Après | Où |
| --- | --- | --- |
| `:composeApp:bundleRelease` | `:androidApp:bundleRelease` | `release-play.yml`, `build.yml` |
| `:composeApp:assembleRelease` | `:androidApp:assembleRelease` | `distribute.yml` |
| `:composeApp:compileDebugKotlinAndroid` | `:composeApp:compileAndroidMain` | `build.yml` |
| `:composeApp:testDebugUnitTest` | `:composeApp:testAndroidHostTest` | `build.yml`, `CLAUDE.md` |
| `:composeApp:assembleDebug` | `:androidApp:assembleDebug` | `CLAUDE.md` |
| `:composeApp:linkDebugFrameworkIosArm64` | **inchangé** | `build.yml` |
| `:composeApp:embedAndSignAppleFrameworkForXcode` | **inchangé** | `iosApp.xcodeproj` (PBXShellScriptBuildPhase) |

Deux conséquences :

- **le projet Xcode n'est pas touché**, à condition que le module KMP garde le nom `composeApp` et que le nouveau module soit `:androidApp` (et non l'inverse) ;
- `composeApp/build/outputs/bundle/release/*.aab` devient `androidApp/build/outputs/bundle/release/*.aab` dans les deux étapes `r0adkll/upload-google-play` ;
- `composeApp/google-services.json` devient `androidApp/google-services.json` — écrit par 3 workflows (`build.yml` ×2, `distribute.yml`, `release-play.yml`).

Le garde-fou `versionCode` (`build.gradle.kts`, rejet Play du 01/08/2026) et les `signingConfigs` déménagent tels quels dans `:androidApp`.

## 6. Ordre d'exécution proposé

1. Créer `:androidApp` (manifeste, `res/`, `MainActivity`, `MibekoApp`, `MyFirebaseMessagingService`, `google-services.json`, `proguard-rules.pro`, signature, `buildTypes`, plugins Firebase) — `composeApp` reste inchangé et cassé à ce stade.
2. Découpler les 5 points du §4 dans `composeApp/src/androidMain`.
3. Basculer `composeApp` sur `com.android.kotlin.multiplatform.library` + `kotlin { android { } }`, retirer le bloc `android { }`.
4. Retirer `android.newDsl=false` et `android.builtInKotlin=false` de `gradle.properties`.
5. Réécrire les 4 workflows et `CLAUDE.md` (§5).
6. Vérifier : `:androidApp:bundleRelease`, `:composeApp:testAndroidHostTest`, `:composeApp:linkDebugFrameworkIosArm64`, et une archive Xcode.

Étape 1 et 2 sont indépendantes de la 3 et peuvent être committées séparément.
