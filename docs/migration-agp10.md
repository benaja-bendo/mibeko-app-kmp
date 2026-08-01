# Migration AGP 10 — séparation KMP / application Android

> Statut : à jour au 2 août 2026 · **Fait autorité sur** : l'état des lieux des dépréciations AGP 9 du dépôt `mibeko-app-kmp` et la migration vers la structure en sous-projets, exécutée le 2 août 2026. Reste : vérifier `distribute-ios.yml` en conditions réelles (§6).

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
| `BuildConfig.DEBUG` (généré par AGP) | `Platform.android.kt:13` | *(diagnostic initial — non retenu, voir « Traitement réel » ci-dessous)* |
| `R.string` / `R.mipmap` | `MyFirebaseMessagingService.kt:88,91` | le fichier part dans `:androidApp` (il importe déjà `MainActivity`) |
| `MibekoApp.INSTANCE` | `Platform.android.kt:16`, `AnalyticsManager.android.kt:12`, `SecureSettings.android.kt:22` | résoudre le `Context` par Koin, comme le font **déjà** `Database.android.kt`, `PlatformUtils.android.kt`, `NetworkConnectivityChecker.android.kt` et `NotificationManager.android.kt` |
| `import MainActivity` | `NotificationManager.android.kt:11` | *(diagnostic initial — non retenu, voir « Traitement réel » ci-dessous)* |
| `res/xml/network_security_config.xml` variante debug | `src/androidDebug/` | part dans `:androidApp`, qui garde les `buildTypes` |
| `debugImplementation(compose.ui.tooling)` | `build.gradle.kts` | part dans `:androidApp` |

**Traitement réel** (le tableau ci-dessus date du diagnostic initial ; deux cases ont été résolues différemment à l'exécution — voir commit `5f938f9`) :

- `BuildConfig.DEBUG` — pas de passage par le plugin `buildConfig` gmazzo (il n'expose que `BASE_URL`, pas un équivalent de `DEBUG`). Retenu : lire `ApplicationInfo.FLAG_DEBUGGABLE` sur le paquet installé via le même `Context` injecté par Koin. Même valeur qu'avant sous la config actuelle, mais nature différente — une lecture à l'exécution plutôt qu'une constante de compilation, qui suppose Koin démarré (seul appelant : `KoinModule.kt`, résolu paresseusement, sans risque aujourd'hui).
- `import MainActivity` dans `NotificationManager.android.kt` — ce n'était pas un intent à réécrire : l'import était déjà mort avant la migration (aucune autre occurrence de `MainActivity` dans le fichier). Simple suppression.

Le gros du travail est donc **déjà fait** : 4 des 7 fichiers `androidMain` qui ont besoin d'un `Context` passent par Koin. Il reste 3 usages de `MibekoApp.INSTANCE` et 2 imports de `MainActivity`.

## 5. Chemins à réécrire hors Gradle

Noms de tâches, relevés sur le banc d'essai :

| Aujourd'hui | Après | Où |
| --- | --- | --- |
| `:composeApp:bundleRelease` | `:androidApp:bundleRelease` | `release-play.yml`, `build.yml` |
| `:composeApp:assembleRelease` | `:androidApp:assembleRelease` | `distribute.yml` |
| `:composeApp:compileDebugKotlinAndroid` | `:androidApp:compileDebugKotlin` (dépend de `:composeApp:compileAndroidMain`) | `build.yml` |
| `:composeApp:testDebugUnitTest` | `:composeApp:testAndroidHostTest` | `build.yml`, `CLAUDE.md` |
| `:composeApp:assembleDebug` | `:androidApp:assembleDebug` | `CLAUDE.md` |
| `:composeApp:linkDebugFrameworkIosArm64` | **inchangé** | `build.yml` |
| `:composeApp:embedAndSignAppleFrameworkForXcode` | **inchangé** | `iosApp.xcodeproj` (PBXShellScriptBuildPhase) |

Deux conséquences :

- **le projet Xcode n'est pas touché**, à condition que le module KMP garde le nom `composeApp` et que le nouveau module soit `:androidApp` (et non l'inverse) ;
- `composeApp/build/outputs/bundle/release/*.aab` devient `androidApp/build/outputs/bundle/release/*.aab` dans les deux étapes `r0adkll/upload-google-play` ;
- `composeApp/google-services.json` devient `androidApp/google-services.json` — écrit par 3 workflows (`build.yml` ×2, `distribute.yml`, `release-play.yml`).

Le garde-fou `versionCode` (`build.gradle.kts`, rejet Play du 01/08/2026) et les `signingConfigs` déménagent tels quels dans `:androidApp`.

## 6. Exécution — faite le 2 août 2026

Ordre réellement suivi (le découplage est passé **avant** la création du module : c'est le seul découpage qui garde l'arbre vert à chaque palier) :

1. **Découpler `composeApp/src/androidMain`** des symboles de l'application (§4) — arbre vert, commit atomique possible seul.
2. **Créer `:androidApp`** et y déplacer manifeste, `res/`, `MainActivity`, `MibekoApp`, `MyFirebaseMessagingService`, `google-services.json`, `proguard-rules.pro`, signature, `buildTypes`, plugins Firebase.
3. **Basculer `composeApp`** sur `com.android.kotlin.multiplatform.library` + `kotlin { android { } }`, bloc `android { }` supprimé.
4. **Retirer** `android.newDsl=false` et `android.builtInKotlin=false`.
5. **Réécrire** les 3 workflows Android et `CLAUDE.md` (§5).

Les étapes 2 à 4 forment un tout indivisible : prises séparément, elles laissent l'arbre rouge.

### Vérifications passées

| Vérification | Résultat |
| --- | --- |
| `:androidApp:assembleDebug` | ✅ APK produit |
| `:androidApp:bundleRelease` (R8, signature, mapping Crashlytics) | ✅ AAB produit |
| `:composeApp:testAndroidHostTest` | ✅ 68 tests, 0 échec — identique au relevé de référence sous `testDebugUnitTest` |
| `:composeApp:linkDebugFrameworkIosArm64` | ✅ |
| `:composeApp:embedAndSignAppleFrameworkForXcode` | ✅ tâche intacte (phase de build Xcode non modifiée) |
| Avertissements de dépréciation AGP | **0** (`-Pandroid.debug.obsoleteApi=true`) |

Manifeste fusionné du bundle release, contrôlé point par point : `package="cg.mibeko.app"`, les 3 permissions du projet, `MibekoApp` / `MainActivity` / `MyFirebaseMessagingService` résolus, `authorities="cg.mibeko.app.provider"`, App Links `mibeko.fr/textes` avec `autoVerify`, schémas `mibeko://`, `networkSecurityConfig`. Le fournisseur `cg.mibeko.app.resources.AndroidContextProvider` est présent : les ressources Compose Multiplatform restent embarquées.

### Reste à faire

- **Archive Xcode** non rejouée localement (elle demande les certificats de signature, qui vivent dans les secrets CI). Le risque est faible — la phase de build Xcode et le nom de tâche Gradle sont inchangés, et `linkDebugFrameworkIosArm64` passe — mais la première exécution de `distribute-ios.yml` après cette migration reste le vrai contrôle.
- **Effet de bord constaté** : le `bundleRelease` de vérification a déclenché `uploadCrashlyticsMappingFileRelease`, qui a envoyé un fichier de mapping à Firebase pour la version fictive `0.0.0-verif` (versionCode 1). Sans conséquence, mais pour les vérifications suivantes : `-x uploadCrashlyticsMappingFileRelease`.
- **Doublon repéré au passage, non traité** (hors périmètre) : le binding Koin `AppConfig` est écrit deux fois à l'identique, dans `MibekoApp` (Android) et `KoinHelper.kt` (iOS). Il gagnerait à descendre dans `commonModule`.
