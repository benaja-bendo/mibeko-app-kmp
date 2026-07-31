# Journal des mises à jour — Mibeko

Ce fichier récapitule, par version, ce qui a changé côté utilisateur. Objectif :
servir de base aux notes de version (stores, TestFlight) et à la communication
publique. Chaque nouvelle mise à jour ajoute une section en haut du fichier.

---

## 1.1.1 — 31 juillet 2026

Première mise à jour depuis le lancement (1.0). Deux chantiers menés en parallèle :
fiabiliser des fonctionnalités qui ne marchaient pas du tout, et ajouter la veille
légale + la fraîcheur automatique du corpus hors-ligne.

### Nouveau

- **Veille légale** : possibilité de suivre un texte et d'être alerté quand il
  est mis à jour ; toucher la notification ouvre directement le texte concerné.
- **Fraîcheur du corpus hors-ligne** : un texte déjà téléchargé se met désormais
  à jour tout seul (au plus une fois par jour, sans consommer de données
  mobiles si le mode hors-ligne est actif). Avant cette version, un texte
  téléchargé restait figé à sa version du jour de l'installation — même après
  une correction ou une republication officielle.
- **Traçabilité de lecture** : en lisant un article hors-ligne, un bandeau
  indique la date de consolidation officielle du texte et la date de la copie
  locale, pour juger en un coup d'œil si elle est encore fiable.
- **Épingler un article pour l'assistant** : un bouton désigne l'article comme
  référence pour l'assistant IA, sans préremplir la question à la place de
  l'utilisateur.
- **Écran de contact** : possibilité d'écrire directement à l'équipe depuis
  l'application.
- **Invitation à noter l'app** après la lecture de trois articles — jamais
  conditionnée à un avis positif.

### Corrigé

- **Recherche hors-ligne** : ne renvoyait strictement aucun résultat depuis le
  tout premier lancement de l'application (bug de jointure SQLite) — alors que
  c'est l'argument central de l'app. Corrigé, avec en prime une meilleure
  gestion des accents (« societe » trouve désormais « société »).
- **Centre de notifications** : restait vide en toutes circonstances (bug de
  désérialisation de la réponse serveur) ; les alertes ne rattachaient pas non
  plus l'appareil au bon compte.
- **Suppressions de dossiers hors-ligne perdues à la déconnexion** : une
  suppression faite hors ligne pouvait disparaître si l'utilisateur se
  déconnectait avant la prochaine synchronisation.
- **Message d'erreur de l'assistant IA** : pouvait inviter à souscrire un
  abonnement inexistant dans l'application ; remplacé par un message clair
  avec un bouton de reprise.
- **Version affichée dans les réglages** : ne correspondait pas toujours à la
  version réellement installée.

### Sous le capot (fiabilité — pas pour la communication publique)

- iOS peut enfin remonter plantages et statistiques d'usage anonymes (avant :
  totalement aveugle côté iOS, aucune mesure, jeton de notification factice
  qui polluait la base de production).
- Protection du parc installé : l'app peut être invitée à se mettre à jour si
  une version est jugée trop ancienne côté serveur — échec silencieux si le
  serveur ne répond pas, jamais bloquant par accident.
- Délais réseau désormais bornés partout (plus d'écran figé indéfiniment sur
  réseau dégradé), avec des marges plus longues conservées pour les
  téléchargements volumineux et le chat.
- Intégration continue : l'app iOS est désormais compilée et vérifiée à
  chaque livraison (elle ne l'était pas avant), et la signature de l'archive
  de distribution a été fiabilisée après plusieurs échecs de CI liés à
  l'ajout de Firebase.

---

## Pistes de communication publique

**À mettre en avant** :

1. *« La recherche hors-ligne marche enfin. »* — le correctif le plus
   important : c'est l'argument central de l'app. Formulation possible :
   « Recherchez vos textes de loi même sans connexion — et retrouvez-les
   vraiment, avec ou sans accents. »
2. **Veille légale** — la nouveauté la plus vendable : « Suivez un texte et
   soyez alerté dès qu'il change. »
3. **Fraîcheur automatique** — argument rassurant pour un usage professionnel :
   « Vos textes téléchargés restent à jour, automatiquement. »
4. **Traçabilité juridique** (date de consolidation affichée) — argument de
   confiance pour les avocats : « Sachez toujours de quand date le texte que
   vous consultez. »

**À éviter** : les détails techniques (SQLite, Crashlytics, CI/CD, migrations
de base de données) et l'aveu direct des bugs (« la recherche ne marchait pas
du tout ») — présenter comme une amélioration, pas comme un aveu.

**Suggestion de texte court** (champ « Nouveautés » App Store / Play Store) :

> Recherche hors-ligne fiabilisée, veille légale pour être alerté des mises à
> jour de vos textes, fraîcheur automatique du corpus téléchargé, et
> traçabilité de la date des textes consultés.

---

## 1.0 — juillet 2026

Première version publique : recherche, lecture et organisation des textes de
loi congolais et OHADA.
