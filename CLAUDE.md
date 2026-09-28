# CLAUDE.md — mibeko-app-kmp

## Contexte
App mobile Mibeko (Android/iOS, Kotlin Multiplatform + Compose Multiplatform, `appId cg.mibeko.app`), en production depuis juillet 2026. Un des 7 dépôts du monorepo Mibeko (legaltech Congo-Brazzaville) — voir le `CLAUDE.md` à la racine du monorepo pour la carte complète. Rôle de l'app dans l'écosystème : **fidéliser** (usage citoyen quotidien), à côté de `mibeko.fr` (vendre) et `app.mibeko.fr` (travailler, poste de travail pro).

## Positionnement (décidé — ne pas rediscuter dans le code)
**La loi est gratuite. L'outil de travail est payant.**
Gratuit (site + mobile + compte gratuit) : corpus, recherche hybride, veille JO, assistant IA de base (avec quota), favoris/collections, dossiers avec échéances et pièces (ouverts à tout compte authentifié depuis mibeko-front#24, 07/09/2026). Payant (`app.mibeko.fr`) : export de dossier, générateur de documents, historique IA illimité.
Le gating serveur existe (`EntitlementsResolver`, `GET /v1/me/entitlements`, `EnsureExportEntitled` sur l'export) — mais le mobile ne consomme aujourd'hui que le quota assistant (`ChatViewModel.assistantQuotaSummary`) ; il ne consomme ni les dossiers riches (kmp#40/#41) ni un gating proactif de l'export (aujourd'hui réactif, sur un 403).

## Règles produit non négociables
1. L'app n'affirme **jamais** qu'un texte n'existe pas. Sur échec réseau/API : « Je n'ai pas pu vérifier » + Réessayer. Un état vide ne s'affiche que sur un `Success` avec liste réellement vide.
2. Aucun libellé ne promet une action que le backend ne fait pas.
3. Un seul nom pour l'IA : **« Assistant Mibeko »**, partout. État actuel (non conforme, à corriger — décision D-021 du registre transverse) : le backend s'appelle « Mibeko IA » et l'app utilise 6 dénominations différentes (ChatScreen, HomeScreen, OnboardingScreen, SearchResultsScreen…). Ne pas ajouter une 7e.
4. Les erreurs ne sont jamais avalées : `printStackTrace` comme seule gestion est interdit — il n'en reste aucune occurrence (résorbées le 29/08/2026), ne pas en réintroduire. Passer par `UiResult` + `MibekoErrorState`, et remonter l'exception par `recordException(e, context = "Classe.fonction")`.
5. Pattern d'erreur standard, **livré** dans `util/UiResult.kt` :
   ```kotlin
   sealed interface UiResult<out T> {
       data object Loading : UiResult<Nothing>
       data class Success<T>(val data: T) : UiResult<T>
       data class Error(val offline: Boolean, val retry: () -> Unit) : UiResult<Nothing>
   }
   ```
   Déployé sur l'Accueil (`homeDataError`) et la Bibliothèque (`homeError`, `searchError`). Invariant : `Error` n'efface jamais les résultats de repli déjà affichés, et un état vide ne s'affiche que sur un `Success` réellement vide. Voir aussi `LocalLegalRepository.SearchResult` (sealed de la couche data).

## Build & tests
Deux modules Gradle : **`:composeApp`** porte tout le code partagé (KMP, plugin `com.android.kotlin.multiplatform.library`, sans variantes de build) et **`:androidApp`** la seule coquille applicative Android (Activity, Application, service FCM, manifeste, ressources, signature, R8). Séparation imposée par AGP 9 — voir `docs/migration-agp10.md`. Le module iOS ne bouge pas : il consomme toujours `:composeApp`.
```bash
./gradlew :androidApp:assembleDebug               # build Android debug
./gradlew :composeApp:testAndroidHostTest         # tests commonMain + Android
./gradlew :composeApp:linkDebugFrameworkIosArm64   # compile ET lie iOS (ce que fait la CI)
./gradlew :composeApp:iosSimulatorArm64Test       # les tests partagés, exécutés par Kotlin/Native
```
CI (`build.yml`, sur `main` et `develop`) : compilation Android debug, tests JVM, AAB release non signé (exercice de R8), et — sur `main` uniquement, les runners macOS coûtant ~10× — compilation + édition de liens du framework iOS puis exécution des tests partagés sur Kotlin/Native. Les deux plateformes n'acceptent pas les mêmes noms de fonction : Native refuse parenthèses et virgules dans un identifiant entre accents graves.

Release : voir `.github/workflows/release-play.yml` (déclenché par un tag `v*.*.*`, canal Play `internal` par défaut) et `distribute-ios.yml` (manuel uniquement, `workflow_dispatch` avec `marketing_version` explicite — jamais automatique). Chaque version livrée doit avoir sa section dans `CHANGELOG.md` **avant** le tag — `release-play.yml` le vérifie désormais et refuse de publier sinon, après avoir rejoué les tests (un tag ne déclenche pas `build.yml`, qui n'écoute que les branches).

## Analytics & observabilité (déjà branché — ne pas réinstaller un SDK)
- **Mobile (Android + iOS)** : Firebase Analytics + Crashlytics, façade unique `MibekoAnalytics` (`util/MibekoAnalytics.kt`), interface `AnalyticsManager` en expect/actual. Invariants à préserver : jamais `setUserId`, jamais le texte d'une requête utilisateur, gating par consentement (préférence + `setAnalyticsCollectionEnabled`). iOS no-op uniquement si `GoogleService-Info.plist` absent du bundle (secret CI `IOS_GOOGLE_SERVICES_PLIST`).
- **Web (site + front)** : Umami auto-hébergé (`stats.mibeko.fr`, provisionné par `vps_infra`), plomberie d'injection déjà écrite côté `mibeko-site` et `mibeko-front` — inactive tant que les secrets CI (`PUBLIC_UMAMI_*` / `VITE_UMAMI_*`) ne sont pas passés en `ARG` Docker.
- **Ne pas proposer PostHog ni Plausible** — décision D-008 du registre transverse (01/08/2026), ce serait un doublon.
- Avant d'ajouter un événement, vérifier qu'il n'existe pas déjà sous un autre nom (`AnalyticsEvents` dans `MibekoAnalytics.kt`).

## Design system
« Forêt » uniquement — déjà l'état du code (`ui/theme/Color.kt` : `#1E6B47` action, `#03271A` marque). Doc de référence : `docs/design-system.md` (pas `DESIGN.md`, qui n'existe plus). « Lex Gold » est une divergence de marque **assumée mais confinée au dashboard web** (`mibeko-front`) — ne jamais l'introduire côté mobile.

## Conventions de travail
- Feuille de route : `docs/produit/feuille-de-route.md` du monorepo (dépôt `mibeko-docs`), section « App mobile » — Maintenant / Ensuite / Plus tard ; l'état des tickets est sur le tableau GitHub (champ « Horizon »).
- Avant de corriger un constat d'audit, **vérifier contre le code actuel** (les références fichier:ligne bougent vite sur ce projet).
- Toute décision structurante s'écrit au format du registre (D-001) : dans `docs/decisions.md` de ce dépôt (préfixe `APP-`) si elle ne change que ce dépôt ; sinon dans le registre transverse, `docs/decisions.md` du monorepo (dépôt `mibeko-docs`, préfixe `D-`).
- Commits en français, format `type(scope): titre court` à l'impératif, corps expliquant le POURQUOI. Un sujet cohérent par commit. Jamais sans l'accord explicite de l'utilisateur.

## Priorités actuelles
Le rôle de l'app est fixé par D-049 (registre transverse, 28/09/2026) : **compagnon grand public** (Assistant, lecture, hors-ligne, alertes), chiffre à suivre = retour dans les 7 jours. **Aucune fonction ne vient sur le mobile parce que le web l'a** ; la parité porte sur les droits (entitlements), jamais sur les écrans. Pas de dossier d'affaire, pas d'achat ni de lien d'achat (D-034). L'ordre des chantiers (publier la 1.4, onglet « Moi » #40, notifications #34…) est dans la feuille de route ci-dessus — ne pas le recopier ici.
