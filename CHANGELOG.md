# Journal des mises à jour — Mibeko

Ce fichier récapitule, par version, ce qui a changé côté utilisateur. Objectif :
servir de base aux notes de version (stores, TestFlight) et à la communication
publique. Chaque nouvelle mise à jour ajoute une section en haut du fichier.

---

## 1.3.0 — 27 août 2026

Version de lisibilité du corpus. Deux chantiers : les tableaux des textes
officiels (annexes budgétaires, grilles de coordonnées, barèmes) et l'intitulé
des décrets publiés « en abrégé » par le Journal officiel.

### Nouveau

- **Les tableaux des textes s'affichent enfin comme des tableaux** : en-têtes
  distingués, montants alignés à droite, défilement horizontal pour les
  tableaux larges. Ils s'affichaient auparavant en balises informatiques, une
  ligne illisible de plusieurs milliers de caractères.
- **Les tableaux et les préambules portent leur vrai nom** : « Tableau 1 »,
  « Préambule », « Signature » au lieu d'« Article TABLEAU_1 ».
- **Les décrets et arrêtés publiés « en abrégé » affichent leur objet** : le
  Journal officiel publie certaines décisions sans objet dans leur titre
  (« Décret n° 2025-240 du 20 juin 2025. »). L'app affiche désormais, à côté
  du titre officiel — jamais à sa place —, l'objet dérivé du corps de l'acte.

### Corrigé

- **Le partage et la copie d'un article** n'envoient plus de balises
  informatiques au destinataire : le tableau part sous forme lisible.

### Sous le capot (fiabilité — pas pour la communication publique)

- Base locale en version 12 : deux migrations additives sans backfill — la
  structure des tableaux (v11) et l'objet dérivé des actes en abrégé (v12) —
  aucune perte du corpus déjà téléchargé.
- Retrait du client de connexion Firebase, sans appelant depuis la suppression
  de l'endpoint côté serveur.

---

## 1.2.0 — 7 août 2026

Version de fiabilité. Le fil conducteur : **l'application ne doit jamais
affirmer qu'un texte n'existe pas alors qu'elle n'a simplement pas pu
vérifier**. Plusieurs écrans affichaient un résultat vide en cas de panne
réseau, ce qui revenait à annoncer une absence de droit. Ils distinguent
désormais « rien trouvé » de « je n'ai pas pu vérifier », avec un bouton
Réessayer. S'y ajoute l'ouverture des liens partagés dans l'application.

### Nouveau

- **Les liens de textes partagés ouvrent l'application** : un lien
  `mibeko.fr/textes/…` reçu par WhatsApp ou par courriel ouvre directement le
  texte dans l'app quand elle est installée, et la page web sinon. Sur iPhone,
  ces liens ouvraient jusqu'ici systématiquement le navigateur.
- **Choix « Citoyen » ou « Professionnel »** à la première connexion, à la
  place de l'ancien écran de profil : deux usages du droit, deux entrées.
- **Partage depuis le site** : les pages de textes de `mibeko.fr` proposent
  désormais un partage direct (partage natif, WhatsApp, copie du lien) — c'est
  le principal chemin d'arrivée de nouveaux utilisateurs vers l'app.

### Corrigé

- **Recherche dans la Bibliothèque** : une coupure réseau affichait « aucun
  résultat », donnant à croire que le texte cherché n'existait pas. L'échec
  est maintenant annoncé comme tel, et réessayable.
- **Centre de notifications** : même confusion entre une boîte réellement vide
  et une panne de connexion.
- **Écran d'accueil** : le contenu documentaire ne s'affichait pas, et les
  erreurs de chargement passaient inaperçues.
- **Ouverture d'un lien partagé** : en cas d'échec de résolution,
  l'application revenait en silence à l'accueil sans rien dire ; elle explique
  désormais ce qui s'est passé et propose de réessayer.
- **Dossiers** : ajout d'un article à un dossier, confirmation avant
  suppression, et erreurs enfin visibles au lieu d'être avalées.
- **Barre de navigation après connexion** : son état ne suivait pas toujours
  la session réellement ouverte.
- **Libellés trompeurs** : un bouton annonçait un effet immédiat qu'il n'avait
  pas, un badge décrivait mal l'état du contenu téléchargé.
- **Signalement d'une anomalie** : la description est maintenant obligatoire,
  un signalement vide n'étant pas exploitable.

### Sous le capot (fiabilité — pas pour la communication publique)

- Application Android extraite dans son propre module (`:androidApp`),
  prérequis de la montée de version des outils de build Android.
- Un build de publication échoue désormais si le numéro de version interne est
  absent, au lieu de produire un paquet que le store refusera.
- Écran de recherche hérité (`ui/search/`) supprimé : injoignable depuis le
  lancement, il dupliquait la Bibliothèque.
- Mesure d'usage de la bannière de mise à jour (clic et fermeture), pour savoir
  si elle sert réellement.
- iOS : déclaration des domaines associés — c'est elle qui manquait pour que
  les liens `mibeko.fr` ouvrent l'app. Nécessite un profil de distribution
  régénéré avec la capacité « Associated Domains ».

---

## Pistes de communication publique — 1.2.0

**À mettre en avant** :

1. **Le partage** — la nouveauté la plus concrète : « Partagez un article de
   loi à un proche : il l'ouvre directement dans l'application. »
2. **La fiabilité de la recherche** — à formuler en positif : « Quand la
   connexion flanche, Mibeko vous le dit au lieu de vous laisser croire que le
   texte n'existe pas. »

**À éviter** : dire que les liens ne fonctionnaient pas du tout sur iPhone, et
tout terme technique (domaines associés, modules de build, réseau).

**Suggestion de texte court** (champ « Nouveautés » App Store / Play Store) :

> Partagez un texte de loi : votre destinataire l'ouvre directement dans
> l'application. Recherche, accueil et notifications plus fiables en connexion
> instable — Mibeko distingue désormais « aucun résultat » de « connexion
> indisponible ».

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
