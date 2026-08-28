# CLAUDE.md — mibeko-app-kmp

## Contexte
App mobile Mibeko (Android/iOS, Kotlin Multiplatform + Compose Multiplatform, `appId cg.mibeko.app`), en production depuis juillet 2026. Un des 7 dépôts du monorepo Mibeko (legaltech Congo-Brazzaville) — voir le `CLAUDE.md` à la racine du monorepo pour la carte complète. Rôle de l'app dans l'écosystème : **fidéliser** (usage citoyen quotidien), à côté de `mibeko.fr` (vendre) et `app.mibeko.fr` (travailler, poste de travail pro).

## Positionnement (décidé — ne pas rediscuter dans le code)
**La loi est gratuite. L'outil de travail est payant.**
Gratuit (site + mobile + compte gratuit) : corpus, recherche hybride, veille JO, assistant IA de base (avec quota), favoris/collections. Payant (`app.mibeko.fr`) : dossiers avec échéances, générateur de documents, exports, historique IA illimité.
⚠️ Ce périmètre n'est pas encore verrouillé côté serveur (aucune route dossiers/assistant/export ne vérifie de rôle) — ne pas construire de nouvelle fonctionnalité mobile en supposant un gating serveur qui n'existe pas encore.

## Règles produit non négociables
1. L'app n'affirme **jamais** qu'un texte n'existe pas. Sur échec réseau/API : « Je n'ai pas pu vérifier » + Réessayer. Un état vide ne s'affiche que sur un `Success` avec liste réellement vide.
2. Aucun libellé ne promet une action que le backend ne fait pas.
3. Un seul nom pour l'IA : **« Assistant Mibeko »**, partout. État actuel (non conforme, à corriger — voir `docs/decisions.md`) : le backend s'appelle « Mibeko IA » et l'app utilise 6 dénominations différentes (ChatScreen, HomeScreen, OnboardingScreen, SearchResultsScreen…). Ne pas ajouter une 7e.
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
./gradlew :composeApp:compileKotlinIosSimulatorArm64  # compile iOS — PAS lancé en CI aujourd'hui (dette connue)
```
Release : voir `.github/workflows/release-play.yml` (déclenché par un tag `v*.*.*`, canal Play `internal` par défaut) et `distribute-ios.yml` (manuel uniquement, `workflow_dispatch` avec `marketing_version` explicite — jamais automatique). Chaque version livrée doit avoir sa section dans `CHANGELOG.md` **avant** le tag.

## Analytics & observabilité (déjà branché — ne pas réinstaller un SDK)
- **Mobile (Android + iOS)** : Firebase Analytics + Crashlytics, façade unique `MibekoAnalytics` (`util/MibekoAnalytics.kt`), interface `AnalyticsManager` en expect/actual. Invariants à préserver : jamais `setUserId`, jamais le texte d'une requête utilisateur, gating par consentement (préférence + `setAnalyticsCollectionEnabled`). iOS no-op uniquement si `GoogleService-Info.plist` absent du bundle (secret CI `IOS_GOOGLE_SERVICES_PLIST`).
- **Web (site + front)** : Umami auto-hébergé (`stats.mibeko.fr`, provisionné par `vps_infra`), plomberie d'injection déjà écrite côté `mibeko-site` et `mibeko-front` — inactive tant que les secrets CI (`PUBLIC_UMAMI_*` / `VITE_UMAMI_*`) ne sont pas passés en `ARG` Docker.
- **Ne pas proposer PostHog ni Plausible** — décision actée dans `docs/decisions.md` (01/08/2026), ce serait un doublon.
- Avant d'ajouter un événement, vérifier qu'il n'existe pas déjà sous un autre nom (`AnalyticsEvents` dans `MibekoAnalytics.kt`).

## Design system
« Forêt » uniquement — déjà l'état du code (`ui/theme/Color.kt` : `#1E6B47` action, `#03271A` marque). Doc de référence : `docs/design-system.md` (pas `DESIGN.md`, qui n'existe plus). « Lex Gold » est une divergence de marque **assumée mais confinée au dashboard web** (`mibeko-front`) — ne jamais l'introduire côté mobile.

## Conventions de travail
- Feuille de route transverse : `docs/produit/feuille-de-route-2026-08.md` (dans le dépôt `docs/`) — exécuter phase par phase.
- Avant de corriger un constat d'audit, **vérifier contre le code actuel** (les références fichier:ligne bougent vite sur ce projet).
- Toute décision structurante = une ligne datée dans `docs/decisions.md` (dépôt `docs/`, transverse aux 7 dépôts).
- Commits en français, format `type(scope): titre court` à l'impératif, corps expliquant le POURQUOI. Un sujet cohérent par commit. Jamais sans l'accord explicite de l'utilisateur.

## Priorités actuelles
Phase 0 faite (push + tag `v1.1.1`) → Phase 1 en cours : pattern `UiResult` déployé écran par écran (Recherche Bibliothèque → Accueil → Notifications → Résolveur de liens), rebranchement des LazyRow `popularCodes`/`recentlyAdded` sur l'accueil, sélecteur « Citoyen / Professionnel » à la place de `ProfileSetup`. Pendant cette phase : pas de Stripe, pas de refonte de surfaces, pas de rebranding, pas de nouveau document stratégique (annexe B du plan).
