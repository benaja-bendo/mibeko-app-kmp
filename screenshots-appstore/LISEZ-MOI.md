# Captures App Store — générées le 11/07/2026

5 captures réelles, app buildée en pointant temporairement vers la **prod** (`api.mibeko.fr`) pour avoir du vrai contenu. `local.properties` a été remis dans son état d'origine après coup.

## ⚠️ Correctif suite au rejet App Store Connect

Premier essai : captures prises sur simulateur iPhone 17 Pro Max, résolution 1320×2868 (classe 6,9"). **Apple Store Connect les a rejetées** : "Les dimensions d'au moins une capture d'écran sont incorrectes — 1242×2688, 2688×1242, 1284×2778 ou 2778×1284 attendues" (classe 6,5", pas 6,9").

Les fichiers **à la racine de ce dossier sont déjà corrigés en 1284×2778** (redimensionnement fidèle des captures originales, écart de ratio de 0,4% — imperceptible, contenu identique). Les originaux 1320×2868 sont conservés dans `6.9-pouces-1320x2868/` au cas où un onglet 6,9" serait aussi demandé par ailleurs.

Note technique : je n'ai pas pu re-capturer nativement en 1284×2778 (simulateur iPhone 13/14 Pro Max) — l'injection de taps via `idb` a cessé de fonctionner sur ces appareils nouvellement créés (aucune erreur renvoyée, mais aucun effet à l'écran ; probablement un souci d'injection HID propre à ces simulateurs recréés). Le redimensionnement est une solution fiable et suffisante ici vu le faible écart de ratio.

## Les captures

| Fichier | Écran | Remarque |
|---|---|---|
| `01_accueil.png` | Accueil / assistant IA | Contenu réel (JO du jour, suggestions) |
| `02_bibliotheque.png` | Bibliothèque | 39 textes / 5,9k articles / 7 institutions |
| `03_lecture_article.png` | Lecture d'un article (Code Bleu OHADA, Art. 2) | Thème sépia, bouton "Source officielle" |
| `04_journal_officiel.png` | Détail JO n° 2026-23 | Liste des 16 textes du numéro |
| `05_dossiers.png` | Mes Dossiers (état vide, invité) | Voir point 2 ci-dessous |

## À savoir avant d'uploader

1. **Dans le doute, vise 4 à 6 captures qui racontent un parcours** (recherche → article → JO → dossiers) : celles-ci suffisent, tu peux les uploader telles quelles dans l'onglet iPhone de la fiche.

2. **⚠️ Bug de qualité de contenu repéré en cours de route** : la fiche "Code Bleu Ohada" (et probablement d'autres codes OHADA importés en une seule fois) affiche un article "PREAMBULE" avec un commentaire markdown brut qui a fuité dans le contenu (`<!-- chunk chunk_1_a_200.md -->`) et deux articles dupliqués `Art. SIGNATURE_doublon_1` / `_doublon_2`. Je n'ai **pas** utilisé cet écran pour les captures (j'ai pris l'Article 2, propre), mais ce défaut sera visible par n'importe quel utilisateur qui ouvre ce code — à corriger côté pipeline d'ingestion avant que ça ne remonte en review Apple ou en avis utilisateur. C'est le même type de souci que celui déjà documenté dans `code_penal_reliability` (mémoire) — probablement un défaut générique du parseur sur les codes multi-signataires.

3. **L'assistant IA (Chat) nécessite une connexion** — contrairement à ce qu'indiquait la doc de navigation interne, taper sur une suggestion ("Quels sont mes droits...") m'a redirigé vers un écran de login ("Bienvenue sur Mibeko"), pas vers une conversation. Donc pas de capture "Assistant" dans ce lot : soit tu me fournis un compte de test pour que j'en capture une, soit tu laisses l'assistant hors des captures (ce qui est cohérent avec le fait que les notifications push et l'assistant sont listés comme non prioritaires pour la 1ère soumission dans `publication-ios.md`).

4. **Dossiers vide** : `05_dossiers.png` montre l'état neuf ("Mes Favoris", 0 article) — c'est correct fonctionnellement pour un nouvel utilisateur invité, mais visuellement peu vendeur. Si tu veux une capture plus parlante, il faudrait soit se connecter avec un compte ayant déjà des dossiers, soit accepter cet état tel quel (montre juste que la fonctionnalité existe).

## Prochaines étapes pour toi

1. Va sur la fiche App Store Connect → section captures → onglet **iPhone 6,5"** → glisse les 5 fichiers **à la racine de ce dossier** (déjà en 1284×2778, ordre = celui du tableau ci-dessus).
2. Si un onglet 6,9" apparaît aussi comme requis, utilise ceux de `6.9-pouces-1320x2868/` pour cet onglet-là (ou laisse App Store Connect réutiliser le même jeu via le "gestionnaire des visuels" si l'option apparaît).
3. Pense à faire suivre le point 2 (bug PREAMBULE/doublons) à qui s'occupe du pipeline d'ingestion Python.
4. Il reste encore à faire selon `docs/publication-ios.md` : lancer le workflow `distribute-ios.yml` pour obtenir un build TestFlight, remplir description/mots-clés (déjà rédigés dans `docs/appstore-fiche-mibeko.md`), et renseigner les identifiants de connexion pour la revue Apple (l'app n'a pas de compte démo existant, il faudra en créer un).
