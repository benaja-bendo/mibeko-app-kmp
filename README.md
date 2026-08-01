# Mibeko Mobile — Lois de la République du Congo

> Statut : à jour au 2 juillet 2026 · application mobile Kotlin Multiplatform (Android + iOS) donnant accès aux textes juridiques du Congo-Brazzaville.

**Mibeko Mobile** est l'application mobile de l'écosystème Mibeko, destinée aux citoyens et à la diaspora du Congo-Brazzaville. Elle offre un accès mobile, structuré et partiellement hors-ligne aux textes législatifs et réglementaires (OHADA/CEMAC, monnaie FCFA/XAF). Elle s'appuie sur l'API Laravel `api.mibeko.fr` partagée avec le site public et le dashboard professionnel.

Elle est développée en **Kotlin Multiplatform (KMP)** avec **Compose Multiplatform** : la quasi-totalité du code (~90 %) est partagée entre Android et iOS.

## Fonctionnalités

- **Bibliothèque et exploration** : parcours des documents et codes de la République.
- **Recherche** : recherche d'articles et de textes par mot-clé.
- **Favoris et téléchargements** : consultation de contenus mis en cache localement (Room) pour un usage hors-ligne.
- **Assistant** : écran de discussion assistée.
- **Dossiers** : espace de travail personnel (modèle encore divergent de celui du web).
- **Compte** : authentification complète (voir ci-dessous).
- **Mode sombre** : thème clair/sombre suivant le système.

## Authentification

L'authentification passe par l'API Laravel (Fortify/Sanctum). Le parcours de compte est complet :

- **Connexion et inscription** classiques.
- **Réinitialisation du mot de passe** : `mot de passe oublié` puis réinitialisation (`forgotPassword` / `resetPassword`).
- **Double authentification (2FA) à la connexion** : lorsque le compte exige un code TOTP, l'API répond en HTTP 423 et l'app présente le défi ; un code de récupération (`recovery_code`) est accepté.
- **Suppression du compte** : possible depuis les réglages (`deleteAccount`), avec confirmation par le mot de passe courant.

Le jeton d'authentification est stocké chiffré (EncryptedSharedPreferences sur Android, Keychain sur iOS) derrière l'interface `Settings`, avec migration automatique depuis l'ancien stockage en clair. Un 401 sur une route authentifiée purge la session locale et ramène à l'écran de connexion.

Limites connues à ce jour : la vérification d'e-mail n'est pas encore implémentée, et certains liens pointent encore vers le domaine mort `mibeko.cg`.

## Architecture et structure des modules

Le projet Gradle (`rootProject.name = "mibeko"`) n'inclut qu'un seul module de code partagé, `:composeApp` (voir `settings.gradle.kts`). Le point d'entrée iOS vit dans le dossier Xcode `iosApp/` ; l'application Android est assemblée depuis la cible `androidTarget` de `:composeApp`.

```
mibeko-app-kmp/
├── composeApp/          # module KMP unique : commonMain (~90 %) + androidMain + iosMain
│   └── src/
│       ├── commonMain/  # UI Compose, ViewModels, data (Ktor/Room), DI (Koin), navigation
│       ├── androidMain/ # Ktor Android, Firebase, activité Android
│       └── iosMain/     # Ktor Darwin, ponts iOS
├── iosApp/              # projet Xcode (framework ComposeApp, bundleId cg.mibeko.app)
└── docs/                # documentation technique (voir ci-dessous)
```

Le code de `commonMain` est organisé par domaine sous `com.mibeko.mibeko` : `ui/*` (écrans et ViewModels par feature : `auth`, `home`, `library`, `search`, `reader`, `settings`, `dossier`, `chat`, `favorites`, `downloads`, `notifications`, `onboarding`, `officialjournal`…), `data/*` (`remote` Ktor, `local` Room, `repository`, `preferences`), `di` (Koin) et `ui/navigation`.

### Pile technique

| Domaine | Choix |
| --- | --- |
| Langage / build | Kotlin 2.3.20, Gradle avec `libs.versions.toml`, JVM toolchain 21 |
| UI | Compose Multiplatform 1.10.3, Material 3 |
| Navigation | `navigation-compose` 2.9.1, destinations type-safe (`@Serializable sealed class Screen`) |
| Réseau | Ktor 3.4.2 (Android / Darwin), contenu négocié JSON |
| Persistance locale | Room 2.8.4 + SQLite bundled |
| Injection de dépendances | Koin 4.1.1 |
| Préférences | multiplatform-settings 1.3.0 |
| Notifications / analytics | Firebase (Messaging, Analytics) côté Android |
| SDK Android | compileSdk/targetSdk 36, minSdk 24 |
| iOS | framework statique `ComposeApp`, bundleId `cg.mibeko.app` |

L'URL de base de l'API est injectée à la compilation via le plugin BuildConfig (`BASE_URL`), avec `https://api.mibeko.fr/api` par défaut ; en développement, définir `mibeko.dev.baseUrl` dans `local.properties`.

### Tester contre un backend local (simulateur ou appareil réel)

`local.properties` n'est pas versionné et contient une IP en dur : elle devient fausse dès que la machine change de réseau. Symptôme typique — l'app affiche « Hors-ligne » / « Je n'ai pas pu vérifier » quel que soit le code testé.

```bash
ipconfig getifaddr en0    # IP LAN courante du Mac → mibeko.dev.baseUrl
```

Puis, côté `mibeko-tableau-de-bord`, lancer l'API en écoutant sur toutes les interfaces (le seul Docker Postgres/MinIO ne suffit pas — il faut le serveur PHP lui-même) :

```bash
php artisan serve --host=0.0.0.0 --port=8000
```

Reconstruire l'app après toute modification de `local.properties` (la valeur est injectée à la compilation).

## Prérequis

- Android Studio récent (avec le plugin Kotlin Multiplatform).
- Xcode 15+ et un Mac pour la cible iOS.
- JDK 21.

## Build

Android (debug) :

```bash
./gradlew :composeApp:assembleDebug
```

iOS : ouvrir `iosApp/` dans Xcode, ou lancer la configuration iOS depuis Android Studio. Le framework partagé est produit par la cible KMP `:composeApp`.

Pour une release Android signée, renseigner un `keystore.properties` **hors du dépôt** — par défaut `../../secrets/mibeko-app-kmp/keystore.properties`, surchargeable via `-PkeystorePropertiesFile=...` ou la variable d'environnement `MIBEKO_KEYSTORE_PROPERTIES` (voir `keystore.properties.template`) — puis :

```bash
./gradlew :composeApp:bundleRelease -PversionCode=<n> -PversionName=<x.y.z>
```

## Documentation

La documentation technique se trouve dans [`docs/`](./docs/) :

- [`docs/README.md`](./docs/README.md) — index de la documentation.
- [`docs/design-system.md`](./docs/design-system.md) — design system (palette forêt, typographie, tokens).
- [`docs/prd-mvp.md`](./docs/prd-mvp.md) — cahier des charges du MVP.
- [`docs/publication-ios.md`](./docs/publication-ios.md) — procédure de publication iOS.

## Licence

Projet sous licence **MIT**. Voir [LICENSE](LICENSE).
