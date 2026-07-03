# Product Requirements Document (PRD)

## Mibeko Mobile – MVP

**Version :** 1.1
**Statut :** En révision
**Dernière mise à jour :** 07-01-2026

---

## 1. Contexte & Problématique

L’accès aux textes législatifs à jour en République du Congo (Brazzaville) reste limité par :

- une connectivité Internet instable ou coûteuse,
- des sources dispersées ou peu ergonomiques,
- l’absence d’outils mobiles adaptés aux usages professionnels et citoyens.

Mibeko Mobile vise à fournir une application mobile fiable, performante et utilisable hors-ligne, servant de référence légale officielle et pratique.

---

## 2. Objectifs Produit

### Objectif principal

Développer une application Android et iOS basée sur **Kotlin Multiplatform (KMP)**, orientée **Offline-First**, permettant un accès rapide, structuré et fiable aux textes législatifs congolais.

### Objectifs secondaires

- Réduire la dépendance à la connexion Internet.
- Améliorer la compréhension et la découvrabilité des textes juridiques.
- Offrir une expérience adaptée aussi bien aux professionnels qu’au grand public.

### Indicateurs de succès (KPIs MVP)

- Temps d’accès à un article < 300 ms en mode hors-ligne.
- ≥ 80 % des fonctionnalités utilisables sans connexion.
- Taux de rétention à 30 jours > 30 %.

---

## 3. Public Cible & Personas

### 3.1 Professionnels du droit

**Exemples :** Avocats, magistrats, juristes d’entreprise, étudiants en droit.

**Besoins clés :**

- Précision et exhaustivité des textes.
- Navigation hiérarchique fidèle aux structures officielles.
- Travail hors-ligne fiable.
- Organisation des articles par dossiers/affaires.
- Notifications lors des mises à jour législatives.

### 3.2 Grand public

**Exemples :** Citoyens, entrepreneurs, locataires, employés.

**Besoins clés :**

- Recherche simple par mots-clés ou thématiques.
- Contenu lisible et compréhensible.
- Découverte guidée (codes, thèmes, favoris).
- Partage facile d’articles.

---

## 4. Périmètre Fonctionnel

### Principe Directeur : "Hybride & Transparent"

L'application fonctionne par défaut en mode connecté afin de garantir la fraîcheur des données. Elle permet toutefois à l'utilisateur de **télécharger à la demande** les contenus juridiques qu’il souhaite rendre disponibles hors-ligne. L’interface doit en permanence indiquer clairement la **source des données** (serveur distant ou stockage local).

---

### FR0 – Onboarding & Clause de non-responsabilité (Disclaimer)

**Description**
Un écran bloquant affiché uniquement lors du tout premier lancement de l'application (et après chaque mise à jour majeure des termes), informant l'utilisateur des limites juridiques et de la responsabilité de l'outil.

**Objectif Juridique** : Décharger le projet Mibeko de toute responsabilité en cas d'erreur d'interprétation ou de déphasage temporel avec le Journal Officiel (JO).

**Critères d’acceptation**

* **Affichage unique & Bloquant :**
    - L'écran s'affiche au premier démarrage. L'accès aux fonctionnalités est strictement impossible sans validation.
    - Un flag local `has_accepted_disclaimer_v1` est stocké.

* **Texte Contractuel obligatoire :**
    - Le message doit être rédigé de manière formelle :
      > *« Mibeko est un outil d'assistance et de consultation. Bien que nous nous efforcions de maintenir les textes à jour, cette application **ne remplace en aucun cas le Journal Officiel de la République du Congo**, qui demeure la seule source juridique faisant foi.
      > 
      > En utilisant cette application, vous reconnaissez que :
      > 1. Les textes fournis le sont à titre informatif.
      > 2. Les développeurs et éditeurs de Mibeko déclinent toute responsabilité en cas de préjudice direct ou indirect résultant d'une erreur, d'une omission ou d'un retard de mise à jour dans les textes.
      > 3. Pour toute procédure judiciaire ou acte officiel, il est impératif de se référer au document papier original du Journal Officiel. »*

* **Interaction :**
    - Case à cocher : *"J'ai lu et je reconnais la primauté juridique du Journal Officiel sur cette application."*
    - Bouton d'action : **« Accepter et Entrer »** (activé uniquement si la case est cochée).


---

### FR1 – Recherche unifiée avec contexte réseau

**Description**
Un moteur de recherche unique qui adapte automatiquement son comportement selon l’état de la connexion réseau et la disponibilité du contenu en local.

**Comportement conditionnel**

1. **Mode en ligne (Online)** :

   - La recherche interroge l’API distante (base nationale complète) ainsi que la base locale.
   - Tous les résultats disponibles sont affichés.

2. **Mode hors-ligne (Offline)** :

   - La recherche interroge uniquement les contenus explicitement téléchargés par l’utilisateur.
   - Si aucun résultat local n’est trouvé, un message explicite est affiché :
     *« Aucun résultat dans vos téléchargements. Connectez-vous pour chercher dans la base nationale. »*

**Critères d’acceptation**

- Indicateur visuel de source :
  - Icône spécifique pour un résultat disponible hors-ligne.
  - Icône distincte pour un résultat nécessitant une connexion Internet.
- Filtre de recherche : option « Uniquement mes textes téléchargés ».

---

### FR2 – Bibliothèque juridique & gestion des téléchargements

**Description**
Consultation des codes et lois avec un contrôle explicite et granulaire de l’espace de stockage utilisé.

**User Story**
« En tant qu’étudiant, je veux télécharger uniquement le *Code de la Famille* et le *Code Pénal* pour économiser mes données, tout en gardant le reste accessible en ligne. »

**Critères d’acceptation**

- Granularité du téléchargement :

  - Téléchargement possible au niveau :
    - d’un Code entier (ex. Code du Travail),
    - d’une Loi spécifique.
  - Le téléchargement article par article est exclu du MVP.

- Feedback visuel clair :

  - Nuage gris : contenu disponible uniquement en ligne.
  - Loader circulaire : téléchargement en cours (avec option d’annulation).
  - Coche verte ou épingle : contenu disponible hors-ligne.

- Gestion de l’espace :

  - Option « Supprimer les téléchargements » dans les paramètres.
  - Les favoris et dossiers ne sont pas supprimés lors du nettoyage.

---

### FR3 – Indicateurs de connectivité & synchronisation

**Description**
L’interface utilisateur doit communiquer en temps réel l’état de la connexion réseau afin d’éviter toute ambiguïté ou frustration.

**Critères d’acceptation**

- Bandeau de statut réseau :

  - Perte de connexion : affichage d’un bandeau discret
    *« Mode hors-ligne. Seuls vos textes téléchargés sont accessibles. »*
  - Retour de connexion : message temporaire
    *« Connexion rétablie. Synchronisation… »*

- États vides (Empty States) :

  - En mode hors-ligne, l’accès à un contenu non téléchargé affiche :
    *« Ce contenu n’est pas disponible hors-ligne. [Réessayer la connexion] »*

- Indicateur de fraîcheur :

  - Affichage de la date de dernière synchronisation pour chaque contenu téléchargé.
  - Notification « Mise à jour disponible » lorsqu’une version plus récente est détectée.

---

### FR4 – Favoris & dossiers (Local First)

**Description**
Les données créées par l’utilisateur (favoris, dossiers) sont stockées localement en priorité afin de garantir leur accessibilité en toutes circonstances.

**Critères d’acceptation**

- Lorsqu’un article est ajouté en favori alors que son Code n’est pas téléchargé, l’article concerné est automatiquement mis en cache local.
- Les dossiers et favoris sont persistés en base locale (Room Multiplatform).

---

### FR5 – Partage, Diffusion & Export

**Description**
Permettre à l'utilisateur de diffuser les textes juridiques de manière professionnelle et fluide via différents canaux.

**Options de partage (Menu Contextuel)**

1.  **Copier le Texte Brut** : 
    - Copie le contenu de l'article dans le presse-papier.
    - **Offline-Safe** : Fonctionne sans connexion si le texte est en cache ou téléchargé.

2.  **Partager le Lien (Deep Link)** : 
    - Génère un lien `https://mibeko.cg/loi/...`.
    - Permet une ouverture directe dans l'application chez le destinataire.

3.  **Exporter en PDF Officiel** :
    - Appel à l'API Laravel (`BE5`) pour générer un document brandé.
    - **Online Required** : Nécessite une connexion pour générer et télécharger le PDF.
    - Affiche un état de chargement ("Génération en cours...") pendant le traitement.

**Critères d’acceptation**
- Utilisation des Share Sheets natives (Android/iOS).
- Formatage propre du texte copié (incluant la source "Source : Mibeko").
- Gestion des erreurs si l'API de génération de PDF est inaccessible.

---

### Note technique (orientation implémentation)

Pour assurer un support hors-ligne granulaire et fiable :

1. Une table locale `Resources` référence l’ensemble des Codes et Lois disponibles (métadonnées uniquement).
2. Un indicateur `is_downloaded` permet de distinguer les contenus réellement stockés localement.
3. L’UI doit être réactive à l’état réseau (ConnectivityManager / NetworkCapabilities via KMP).
4. Les téléchargements interrompus doivent être détectés et repris ou invalidés proprement.

---

## 5. Exigences Non Fonctionnelles (NFR)

### Performance

- Fonctionnement fluide sur appareils Android d’entrée de gamme (≥ 4 Go RAM).
- Temps d’ouverture de l’application < 2 secondes.

### Sécurité & intégrité

- Chiffrement de la base locale.
- Protection contre l’altération non autorisée des textes.

### Compatibilité

- Android (API 26+).
- iOS (versions supportées par KMP).

---

<!-- ## 6. Hors périmètre du MVP

- Interprétation juridique personnalisée.
- Consultation avec des professionnels.
- Création de contenu par les utilisateurs. -->

---

## 7. Évolutions Futures (V2+)

- Recherche assistée par IA (question en langage naturel).
- version lingala des textes.
- Résumés et explications vulgarisées des articles.
<!-- - Forum communautaire d’entraide juridique. -->
- Comptes utilisateurs synchronisés (multi-appareils).
<!-- - Version Web complémentaire. -->

---

## 8. Risques & Hypothèses

**Hypothèses**

- Les textes juridiques officiels sont disponibles dans un format exploitable.
- Les mises à jour légales sont identifiables de manière fiable.

**Risques**

- Volume croissant de données.
- Complexité de synchronisation offline.
- Responsabilité liée à l’exactitude des textes.

