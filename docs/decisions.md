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
