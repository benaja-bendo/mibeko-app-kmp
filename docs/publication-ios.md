# Guide de publication iOS — Mibeko (App Store)

Guide pas-à-pas pour publier l'app iOS Kotlin Multiplatform / Compose sur l'App Store, avec les corrections de production déjà appliquées et tout ce qu'il reste à faire.

- **Bundle ID** : `cg.mibeko.app`
- **Apple Team ID** : `G2VC572UTM`
- **App Store Connect Key ID (déjà sur ta machine)** : `8ZTFD7S36Q` (fichier `AuthKey_8ZTFD7S36Q.p8`)

> ⚠️ Apple est **plus strict que Google** sur les apps d'information juridique/gouvernementale (Guideline 5.2.3). Le même correctif que pour Android s'applique : sources officielles + disclaimer. Bonne nouvelle : le composant `OfficialSourcesSheet` est dans le code **partagé** (`commonMain`), donc il est **déjà présent dans l'app iOS** (Paramètres, Lecteur, Journal Officiel).

---

## 0. État du projet — ce que j'ai déjà corrigé

| Problème détecté | Gravité | Statut |
|---|---|---|
| Icône App Store `1024.png` avec **canal alpha** → rejet automatique à l'upload (ITMS-90717) | 🔴 Bloquant | ✅ Corrigé (37 icônes mises à plat, RGB sans alpha, aucun changement visuel) |
| `Info.plist` figeait `CFBundleVersion = 1` → la CI ne pouvait **jamais** incrémenter le build → 2ᵉ upload TestFlight impossible | 🔴 Bloquant CI | ✅ Corrigé (`$(CURRENT_PROJECT_VERSION)` / `$(MARKETING_VERSION)`) |
| `IPHONEOS_DEPLOYMENT_TARGET = 18.2` → app installable **quasiment par personne** | 🟠 Critique (audience) | ✅ Corrigé → `16.0` |
| **Privacy manifest absent** (`PrivacyInfo.xcprivacy`) → e-mails Apple ITMS-91053 / risque de rejet | 🟠 Important | ✅ Créé (`iosApp/iosApp/PrivacyInfo.xcprivacy`) |
| Nom d'app affiché « mibeko » (minuscule) | 🟡 Cosmétique | ✅ Corrigé (`CFBundleDisplayName = Mibeko`) |
| Sources officielles + disclaimer dans l'app | ✅ | Déjà là (code partagé KMP) |
| ATS (HTTPS forcé, `ITSAppUsesNonExemptEncryption=false`) | ✅ | Déjà correct |
| Jeu d'icônes complet (toutes tailles) | ✅ | Déjà présent |

**Fichiers modifiés/créés :**
- `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/*.png` (alpha retiré)
- `iosApp/iosApp/Info.plist` (versioning + display name)
- `iosApp/iosApp.xcodeproj/project.pbxproj` (deployment target 16.0)
- `iosApp/iosApp/PrivacyInfo.xcprivacy` (nouveau)

➡️ **Après avoir ouvert le projet dans Xcode une fois**, vérifie que `PrivacyInfo.xcprivacy` est bien dans la target : sélectionne le fichier → *File Inspector* (panneau droit) → **Target Membership** → `iosApp` coché. (Avec les « dossiers synchronisés » Xcode 16 utilisés ici, c'est automatique, mais vérifie.)

---

## 1. Une décision à prendre : iPhone seul ou iPhone + iPad ?

Aujourd'hui le projet cible **iPhone + iPad** (`TARGETED_DEVICE_FAMILY = "1,2"`).

| Option | Conséquences |
|---|---|
| **iPhone seul** (recommandé pour le v1) | Moins de captures à fournir, pas de revue iPad, lancement plus rapide. |
| iPhone + iPad (actuel) | Tu **dois** fournir des captures iPad 13", et l'UI Compose doit être nickel sur grand écran (sinon rejet « qualité iPad »). |

👉 Pour publier vite et sans accroc, **passe en iPhone seul** : dans `iosApp/iosApp.xcodeproj/project.pbxproj`, remplace les deux `TARGETED_DEVICE_FAMILY = "1,2";` par `TARGETED_DEVICE_FAMILY = "1";` (ou via Xcode : target *iosApp* → *General* → *Supported Destinations* → retire iPad). Tu pourras rajouter l'iPad plus tard.

---

## 2. Prérequis (comptes)

1. **Apple Developer Program** actif : 99 $/an — https://developer.apple.com/programs/
2. **App Store Connect** : https://appstoreconnect.apple.com/
3. **Xcode** récent installé (tu es sur macOS) + connecté à ton compte Apple (Xcode → Settings → Accounts).

---

## 3. Créer la fiche de l'app dans App Store Connect

1. https://appstoreconnect.apple.com/ → **Apps** → **+** → **Nouvelle app**.
2. Plateforme **iOS**, nom **Mibeko**, langue principale **Français (France)**, **Bundle ID** `cg.mibeko.app` (s'il n'apparaît pas, crée-le d'abord dans le portail développeur — voir §6.B), SKU libre (ex. `mibeko-ios-001`).
3. Renseigne ensuite : catégorie (**Références** ou **Actualités** / secondaire **Productivité**), URL d'assistance, URL de politique de confidentialité (obligatoire).

---

## 4. Conformité « informations gouvernementales » (Apple Guideline 5.2.3)

Comme pour Google Play, deux exigences — **réutilise exactement le texte validé pour Android** :

- **Description App Store** : inclure le bloc **SOURCES OFFICIELLES** (https://www.sgg.cg et https://www.ohada.org) + le bloc **AVERTISSEMENT** « initiative privée, non affiliée au gouvernement ». (Reprends le texte consolidé de la fiche Play.)
- **Dans l'app** : ✅ déjà fait — `OfficialSourcesSheet` (Paramètres → « Sources officielles », pied du Lecteur, pied du Journal Officiel) est dans le code partagé donc présent sur iOS.

📎 App Store Review Guidelines : https://developer.apple.com/app-store/review/guidelines/ (voir section 5.2.3).

---

## 5. Images & captures d'écran (le point sur lequel tu comptes sur moi)

### 5.1 Icône — ✅ rien à faire
L'icône App Store (1024×1024) est désormais **sans alpha** et incluse dans l'asset catalog. App Store Connect la récupère **automatiquement depuis le build** ; tu n'as rien à uploader séparément.
📎 Réf. : https://developer.apple.com/design/human-interface-guidelines/app-icons

### 5.2 Captures d'écran — à produire
Apple impose au minimum **un jeu de captures iPhone 6.9"/6.7"**. Tailles acceptées (portrait) :

| Appareil | Résolution (px) | Simulateur conseillé |
|---|---|---|
| iPhone **6.9"** | **1320 × 2868** | iPhone 16 Pro Max |
| iPhone **6.7"** | **1290 × 2796** | iPhone 15 Pro Max |
| iPad **13"** (seulement si tu gardes l'iPad) | **2064 × 2752** ou **2048 × 2732** | iPad Pro 13" (M4) |

- **Nombre** : 3 à 10 captures par taille (vise 4–6 qui racontent : recherche, lecture d'un article, mode hors-ligne, Journal Officiel, dossiers).
- **Comment les générer proprement** :
  1. Xcode → choisis le simulateur **iPhone 16 Pro Max** → lance l'app (`Cmd+R`).
  2. Mets l'app dans les écrans voulus → menu Simulateur **File → Save Screen** (ou `Cmd+S`). L'image est **déjà à la bonne résolution**.
  3. Glisse-les dans App Store Connect (onglet de la version → *Aperçus et captures d'écran*).
- ❌ Pas de coins transparents, pas de mockups trompeurs ; ce sont des captures réelles de l'app.

📎 Spécifications officielles (toujours à jour) : https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/

### 5.3 Texte de la fiche
Nom **Mibeko**, sous-titre court (30 car. max, ex. « Le droit congolais simplifié »), description (= celle de Play avec sources + disclaimer), mots-clés (100 car., ex. `droit,loi,congo,ohada,juridique,code,avocat,légal`), nouveautés de version.

---

## 6. Construire et envoyer le build

Tu as **trois voies**. Pour la **première soumission**, la voie **A (Xcode manuel)** est la plus fiable.

### A. Voie manuelle via Xcode (recommandée la 1re fois)

1. Ouvre `iosApp/iosApp.xcodeproj` dans Xcode.
2. Vérifie : target *iosApp* → *Signing & Capabilities* → **Automatically manage signing** coché, Team = `G2VC572UTM`.
3. En haut, choisis la destination **Any iOS Device (arm64)** (pas un simulateur).
4. Menu **Product → Archive**. (Le script Gradle `embedAndSignAppleFrameworkForXcode` compile le framework Kotlin ; prévois quelques minutes la 1re fois — il faut **Java 21** installé : `brew install openjdk@21`.)
5. À la fin, l'**Organizer** s'ouvre → sélectionne l'archive → **Distribute App** → **App Store Connect** → **Upload** → laisse la signature automatique → **Upload**.
6. Le build apparaît dans App Store Connect → **TestFlight** après ~5–15 min de traitement.

### B. Voie CI : GitHub Actions (workflow `distribute-ios.yml` déjà présent)

Le workflow `.github/workflows/distribute-ios.yml` build + signe + envoie sur **TestFlight** (déclenchement manuel via l'onglet *Actions* → *Distribute iOS to TestFlight* → *Run workflow*).

> ✅ Grâce au correctif `Info.plist`, le numéro de build (`github.run_number + 25`) est maintenant **réellement** pris en compte — avant, il restait coincé à 1.

**Secrets GitHub à configurer** (*Settings → Secrets and variables → Actions*) :

| Secret | Quoi / comment l'obtenir |
|---|---|
| `BUILD_CERTIFICATE_BASE64` | Ton certificat **Apple Distribution** au format `.p12` encodé : `base64 -i Certificats.p12 \| pbcopy` |
| `P12_PASSWORD` | Le mot de passe choisi à l'export du `.p12` |
| `BUILD_PROVISION_PROFILE_BASE64` | Le profil d'appro **App Store** pour `cg.mibeko.app` (`.mobileprovision`) téléchargé du portail développeur, puis `base64 -i ton_profil.mobileprovision \| pbcopy` |
| `APP_STORE_CONNECT_API_KEY` | Le **contenu** du fichier `AuthKey_8ZTFD7S36Q.p8` (colle tout le texte) |
| `APP_STORE_CONNECT_KEY_ID` | `8ZTFD7S36Q` |
| `APP_STORE_CONNECT_ISSUER_ID` | App Store Connect → *Users and Access* → *Integrations* → *App Store Connect API* → **Issuer ID** |

📎 Clés API App Store Connect : https://developer.apple.com/documentation/appstoreconnectapi/creating-api-keys-for-app-store-connect-api

> Notes CI : le workflow utilise JDK 17 — le projet cible Java 21 (`jvmToolchain(21)`). Si le build échoue côté Gradle, change `java-version: '17'` → `'21'` dans `distribute-ios.yml` et dans `iosApp/ci_scripts/ci_post_clone.sh` (`openjdk@17` → `openjdk@21`).

### C. (Option) Xcode Cloud
Le script `iosApp/ci_scripts/ci_post_clone.sh` est prêt pour Xcode Cloud (installe Java avant le build Gradle). Si tu préfères Xcode Cloud à GitHub Actions, configure-le depuis Xcode → *Report navigator* → *Cloud*. (Là aussi : passe à `openjdk@21`.)

---

## 7. TestFlight → Soumission à la revue

1. Dans **TestFlight**, réponds au questionnaire **Export Compliance** : l'app n'utilise que du chiffrement standard (HTTPS) → exempt. *(Déjà déclaré : `ITSAppUsesNonExemptEncryption = false`, donc pas de question répétée.)*
2. (Recommandé) Teste le build via TestFlight sur ton iPhone avant de soumettre.
3. Onglet **App Store** → ta version **1.0** → renseigne tout : captures (§5.2), description (§4–5.3), **App Privacy** (§8), classification d'âge, coordonnées de contact.
4. Sélectionne le build TestFlight.
5. **Add for Review** → **Submit**.
6. Délai de revue : généralement 24–48 h.

📎 TestFlight : https://developer.apple.com/testflight/

---

## 8. Confidentialité (App Privacy) — questionnaire App Store Connect

Dans la fiche : *App Privacy* → *Edit*. Déclare en cohérence avec le `PrivacyInfo.xcprivacy` :
- **Données collectées** : *Contact Info → Email Address* et *Name* — usage **App Functionality**, **liées** à l'utilisateur, **pas** pour du tracking.
- **Tracking** : **Non**.

📎 https://developer.apple.com/app-store/app-privacy-details/
📎 Privacy manifests : https://developer.apple.com/documentation/bundleresources/describing-data-use-in-privacy-manifests
📎 API à raison requise : https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api

> Si Apple t'envoie un e-mail **ITMS-91053** mentionnant une catégorie d'API non déclarée, ajoute la catégorie citée dans `PrivacyInfo.xcprivacy` (section `NSPrivacyAccessedAPITypes`) et renvoie un build.

---

## 9. Checklist finale avant « Submit »

- [ ] (Décision) iPhone seul ou iPad inclus → captures correspondantes fournies
- [ ] Build uploadé et visible dans TestFlight
- [ ] Description avec **SOURCES OFFICIELLES** + **AVERTISSEMENT** (identique à Play)
- [ ] Captures iPhone 6.9"/6.7" (+ iPad si universel)
- [ ] App Privacy rempli (email, nom ; pas de tracking)
- [ ] URL de confidentialité + URL d'assistance renseignées
- [ ] Classification d'âge complétée
- [ ] Export compliance répondu
- [ ] `PrivacyInfo.xcprivacy` bien dans la target (vérif Xcode)
- [ ] Changements committés sur Git

---

## 10. Limites connues / pistes (non bloquantes pour la 1re soumission)

- **Notifications push iOS non configurées** : `iOSApp.swift` n'initialise pas Firebase et il n'y a ni entitlement `aps-environment` ni `GoogleService-Info.plist` côté iOS. Les « Alertes juridiques » ne fonctionneront donc pas encore sur iPhone. Ce n'est pas un motif de rejet, mais évite de trop insister dessus dans la description iOS, ou ajoute le support APNs plus tard (capability *Push Notifications* + Firebase iOS + entitlement).
- **Deep links** : le schéma `mibeko://` est branché (`onOpenURL` → `ExternalUriHandler`). Pour des *Universal Links* (https), il faudrait un fichier `apple-app-site-association` sur `api.mibeko.fr` + l'entitlement *Associated Domains* (optionnel).
- **Java 21** requis localement pour archiver (`brew install openjdk@21`).

---

### Références Apple utiles
- App Store Review Guidelines : https://developer.apple.com/app-store/review/guidelines/
- Spécifications captures d'écran : https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications/
- Icônes (HIG) : https://developer.apple.com/design/human-interface-guidelines/app-icons
- Privacy manifests : https://developer.apple.com/documentation/bundleresources/describing-data-use-in-privacy-manifests
- Clés API App Store Connect : https://developer.apple.com/documentation/appstoreconnectapi/creating-api-keys-for-app-store-connect-api
- TestFlight : https://developer.apple.com/testflight/
