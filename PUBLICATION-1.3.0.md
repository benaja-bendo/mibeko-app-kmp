# Publier la version 1.3.0 — procédure

> Préparée le 10 août 2026 · **Mise à jour le 27 août 2026** (2 commits
> supplémentaires découverts, dont une 2e migration) · Fichier de travail, à
> supprimer une fois la version publiée.

## Ce qu'il y a dans cette version

Cinq commits depuis `v1.2.0` (7 août) — les deux derniers n'étaient pas connus
lors de la rédaction initiale de ce document :

| Commit | Ce que ça change |
| --- | --- |
| `2a665ae` | Retrait du client de connexion Firebase — code mort, aucun appelant (contrepartie d'une suppression côté API) |
| `041a96f` | Les tableaux du corpus s'affichent en vrai tableau au lieu de balises ; les feuilles techniques se libellent « Tableau 1 » au lieu d'« Article TABLEAU_1 » ; le partage et la copie ne contiennent plus de HTML |
| `83e9536` | La structure des tableaux est synchronisée et stockée hors-ligne — **migration de base locale 10 → 11** |
| `dc0a958` | Ce fichier lui-même (docs, sans effet applicatif) |
| `c65d5d3` | Affiche l'objet des actes « en abrégé » du JO à côté du titre officiel — **migration de base locale 11 → 12** |

## Faut-il publier ?

**Oui**, mais par le canal interne d'abord. Le contenu est petit et vérifié (105 tests Kotlin, iOS compile), et un des cinq commits ne fait que supprimer du code mort.

**Le vrai risque est la double migration de base locale 10 → 11 → 12.** Chacune ajoute des colonnes nullables, sans backfill, exactement comme la migration 8 → 9 déjà publiée en v1.2.0 (`ALTER TABLE … ADD COLUMN … TEXT DEFAULT NULL`) — un motif éprouvé sur le parc réel, appliqué ici deux fois en cascade. Mais trois choses méritent d'être sues :

- **Aucun test de migration n'existe dans ce projet** (ni pour celles-ci, ni pour les dix précédentes). La vérification est donc manuelle, et c'est l'objet de l'étape 3.
- **Une migration ratée à la montée fait planter l'ouverture de la base**, pas une perte silencieuse : `fallbackToDestructiveMigrationOnDowngrade` ne couvre que les descentes de version. C'est le bon comportement — un plantage se voit, un corpus effacé en silence ne se voit pas — mais il faut donc l'attraper avant la production.
- **La cascade 10→11→12 doit s'exécuter en une fois** au premier lancement pour un appareil encore en v1.2.0 (donc en base v10) : c'est ce chemin précis qu'il faut vérifier, pas seulement 11→12 isolément.

**Le contrôle qui compte** : installer la nouvelle version **par-dessus une v1.2.0 existante**, pas sur un téléphone vierge. Une installation neuve crée la base directement en version 12 et n'exerce jamais les migrations — elle ne prouve rien.

## Étape 1 — Écrire la section du CHANGELOG

**Fait** (27/08) : `CHANGELOG.md` contient désormais la section `## 1.3.0 — 27
août 2026`, mise à jour pour couvrir les 5 commits (tableaux **et** libellés
d'actes en abrégé, double migration 10→11→12). Reste à committer à l'étape 2.

Sur le numéro : **1.3.0** parce que l'app sait faire quelque chose qu'elle ne savait pas faire. `1.2.1` se défendrait si vous considérez que c'est une correction d'affichage — c'est votre appel, mais le tag et le CHANGELOG doivent dire la même chose.

## Étape 2 — Committer, taguer, publier en interne

```bash
cd ~/Desktop/Mibeko/mibeko/mibeko-app-kmp && git add CHANGELOG.md && git commit -m "docs(changelog): section 1.3.0"
```

```bash
cd ~/Desktop/Mibeko/mibeko/mibeko-app-kmp && git push origin main
```

```bash
cd ~/Desktop/Mibeko/mibeko/mibeko-app-kmp && git tag v1.3.0 && git push origin v1.3.0
```

Le tag déclenche `release-play.yml`, qui publie **par défaut sur le canal interne** — immédiat, sans revue Google. Le `versionCode` est calculé automatiquement (nombre de commits), il ne peut pas entrer en collision.

Suivre le résultat :

```bash
gh run watch -R benaja-bendo/mibeko-app-kmp
```

## Étape 3 — Le contrôle qui décide de la suite

**Sur un téléphone qui a déjà l'app en v1.2.0, avec du corpus téléchargé.** Ne pas désinstaller avant, ne pas tester sur un appareil vierge : c'est toute la question.

1. Installer la 1.3.0 depuis le canal interne (Play Store, section « Test interne »).
2. Ouvrir l'app. **Elle ne doit pas planter au démarrage** — c'est là que la cascade de migrations 10→11→12 s'exécute.
3. Ouvrir un texte déjà téléchargé et vérifier que **son contenu est toujours là**.
4. Ouvrir le décret n° 59-183 (ou n'importe quel article « Tableau 1 ») : le tableau doit s'afficher en colonnes, avec défilement horizontal.
5. Partager cet article par WhatsApp vers vous-même : le message ne doit contenir **aucune balise**.
6. Ouvrir la Bibliothèque et trouver un décret publié « en abrégé » (titre du type « Décret n° … du … », sans objet) : l'objet dérivé doit s'afficher à côté du titre officiel, jamais à sa place. Si aucun n'est encore synchronisé localement, forcer une resynchronisation du catalogue.
7. Lancer une recherche : le corpus doit répondre normalement.

Si le point 2, 3 ou 6 échoue, **ne pas publier en production** — dites-le moi, la migration se corrige et se republie sous un nouveau tag.

## Étape 4 — Production, en douceur

Depuis l'onglet **Actions** de GitHub → workflow **« Release to Google Play »** → **Run workflow** :

- `versionName` : `1.3.0`
- `track` : `production`

La production part en **rollout progressif à 20 %** : une régression ne touche qu'un cinquième du parc et se complète — ou s'interrompt — depuis la Play Console.

## Étape 5 — Une fois le rollout à 100 %

Porter le rollout à 100 % dans la Play Console, **puis seulement** lancer le workflow **« Annoncer la version mobile (force-update) »** avec `1.3.0`.

Cet ordre n'est pas une formalité : ce workflow allume la bannière de mise à jour dans l'app pour **tout** le parc. Lancé pendant un rollout à 20 %, il enverrait 80 % de vos utilisateurs vers un bouton « Mettre à jour » qui ne mène nulle part.

## iOS — séparé, et à décider à part

`distribute-ios.yml` est **manuel uniquement**, vers TestFlight, avec une `marketing_version` explicite (App Store Connect refuse une soumission dont la version n'a pas été incrémentée).

Deux choses à savoir avant de le lancer :

- **La compilation iOS n'est pas dans la CI** (dette connue du dépôt). Je l'ai compilée localement le 9 août sur ces changements — `compileKotlinIosSimulatorArm64`, build réussi — mais rien ne le revérifiera automatiquement au moment de la distribution.
- **`latest_version` est un champ unique partagé** entre Android et iOS côté API. Si vous annoncez 1.3.0 (étape 5) sans avoir sorti la contrepartie iOS, les utilisateurs iPhone verront une bannière vers une version qui n'existe pas encore pour eux. La bannière n'est pas bloquante, donc c'est supportable — mais autant le savoir, et idéalement sortir les deux avant d'annoncer.

## Si quelque chose tourne mal

- **Plantage au démarrage après mise à jour** → migration en cause. Interrompre le rollout dans la Play Console, me le signaler. Le correctif est un nouveau tag ; on ne peut pas « dépublier » une version Play, seulement la remplacer.
- **Corpus vidé** → ne devrait pas arriver (le repli destructif ne couvre que les descentes de version), mais ce serait le même geste : interrompre le rollout, signaler.
- **Tableaux affichés en lignes `A | B | C`** → ce n'est pas un bug : c'est un corpus téléchargé avant la bascule qui n'a pas encore resynchronisé. Il se met à jour tout seul ; forcer en rafraîchissant le texte depuis la bibliothèque.
