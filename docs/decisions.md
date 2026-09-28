# Registre des décisions — application mobile (mibeko-app-kmp)

> Statut : à jour au 28 septembre 2026 · **Fait autorité sur** : les décisions en vigueur qui ne changent que le code de ce dépôt. Les décisions qui touchent plusieurs dépôts (compte unique, favoris et hors-ligne, partage par URL, crédits achetés sur le web, e-mail obligatoire, tableaux, analytics…) sont dans le registre transverse (`docs/decisions.md` du monorepo, dépôt `mibeko-docs`), qui donne aussi le gabarit et les règles (D-001).

Identifiants `APP-NNN`, jamais réutilisés ; une nouvelle décision s'ajoute à la fin. Les décisions reprises le 28/09/2026 ne portent « Écarté » et « On rouvre si » que si l'original les donnait ; texte d'origine : `docs/_archive/2026-09-28-journal-decisions-2026-07-a-09.md` (dépôt `mibeko-docs`).

### APP-001 · 2026-09-13 · Sans préférence enregistrée, l'application s'ouvre en thème clair
**Statut** : en vigueur

**Décision** : l'absence de préférence vaut `LIGHT`, jamais le thème système. Un choix explicite `SYSTEM` ou `DARK` reste respecté.
**Contexte** : sur un téléphone en mode sombre, le thème système rendait l'application noire dès le premier lancement.

### APP-002 · 2026-09-12 · Onboarding mobile : la progression du serveur l'emporte, les écritures en attente sont isolées par compte
**Statut** : en vigueur · **Réf.** : kmp#45, D-022

**Décision** :
- **reprise** : le mobile consulte toujours `GET /v1/onboarding/journey`. Un parcours terminé sur le web ne réapparaît pas, et l'ancien `onboarding_completed` de l'appareil ne qualifie jamais un compte ;
- **écritures en attente** : elles sont stockées sous une clé de compte, persistées avant l'appel et rejouées à la reconnexion, jamais visibles depuis un autre compte ;
- **en cas d'échec** (étape inconnue, configuration absente ou réseau en panne), l'accès au produit est conservé ;
- **notifications** : aucune permission n'est demandée à l'entrée.

### APP-003 · 2026-09-28 · La barre du bas passe à quatre onglets : Accueil, Bibliothèque, Assistant, Moi
**Statut** : en vigueur · **Réf.** : kmp#40, D-049

**Contexte** : l'Assistant est la fonction réellement utilisée (16 personnes, 51 questions en septembre, `mibeko:kpis` du 27/09), alors qu'il n'est accessible que depuis l'accueil. Les favoris et les classeurs sont presque inutilisés (2 comptes chacun, mesure du 28/09).
**Décision** : l'onglet « Dossiers » et l'onglet « Profil » disparaissent au profit d'« Assistant » et de « Moi ». « Moi » regroupe le compte, les alertes, les favoris et les collections (D-051), plus la carte Mibeko Apps pour un profil professionnel.
**Écarté** : trois onglets avec l'Assistant accessible seulement depuis l'accueil (version initiale de #40), qui cacherait la fonction utilisée derrière celles qui ne le sont pas.
**On rouvre si** : Firebase montre, deux mois après la publication, que l'onglet Assistant attire moins d'usages que l'entrée par l'accueil.

### APP-004 · 2026-09-28 · Notifications : l'autorisation du système vaut consentement, demandée au bon moment
**Statut** : en vigueur · **Réf.** : kmp#34 (option A)

**Contexte** : le réglage des notifications est désactivé par défaut, et l'autorisation n'est demandée que depuis les Réglages. Au 27/09, 3 appareils sont inscrits, et aucun n'a été vu depuis 30 jours.
**Décision** : le réglage de l'application est activé par défaut. L'application demande l'autorisation du système à un moment utile, par exemple après le premier téléchargement d'un texte, jamais à l'ouverture. La fenêtre d'Android 13 et plus, et celle d'iOS, recueillent le consentement ; couper les notifications dans l'application désinscrit toujours l'appareil côté serveur.
**Écarté** : garder l'activation manuelle en la rendant visible (option B), qui laisserait la veille à une petite minorité.
**On rouvre si** : les désinstallations ou les refus d'autorisation montent après la publication (Firebase, Play Console). Il faudrait alors revoir ce qu'on envoie (une alerte par texte publié) avant d'insister.

### APP-005 · 2026-09-28 · Le code du mobile passe aussi par une PR courte, depuis un worktree
**Statut** : en vigueur · **Réf.** : D-005

**Contexte** : D-005 impose la PR courte aux quatre dépôts qui déploient à chaque push sur `main`. Ici, une fusion ne publie rien (seul un tag part vers les stores), mais la CI (`build.yml`) compile Android et lance les tests sur chaque PR.
**Décision** : une branche `type/numéro-slug` par ticket, dans `mibeko/.worktrees/kmp-<numéro>`, puis une PR courte. La CI ne compile iOS qu'après la fusion : avant de fusionner, lancer `./gradlew :composeApp:linkDebugFrameworkIosArm64` en local. Les versions sont regroupées par jalons GitHub (« 1.4 », « 1.5 »).
