# Documentation — Mibeko Mobile (KMP)

> Statut : à jour au 2 juillet 2026 · index de la documentation technique de l'application mobile Compose Multiplatform (Android + iOS).

Ce dossier regroupe la documentation de l'application mobile Mibeko, destinée aux citoyens et à la diaspora du Congo-Brazzaville. Chaque document est daté et destiné à évoluer avec le code.

## Architecture en bref

- **Compose Multiplatform** (Android + iOS) : l'interface et la logique sont écrites une seule fois en Kotlin, la quasi-totalité du code (~90 %) vit dans `composeApp/src/commonMain`. Les modules `androidMain`/`iosMain` ne portent que les implémentations spécifiques à chaque plateforme (client HTTP, Firebase, point d'entrée natif).
- **Ktor + Room + Koin** : Ktor pour l'accès à l'API Laravel, Room (SQLite bundled) pour le cache local et le mode hors-ligne, Koin pour l'injection de dépendances.
- **Navigation type-safe** : `navigation-compose` avec des destinations déclarées en `@Serializable sealed class Screen`, ce qui garantit des arguments typés à la compilation.

## Documents

| Document | Description |
| --- | --- |
| [design-system.md](./design-system.md) | Palette forêt, typographie (Inter + Source Serif 4), tokens et principes de composants du design system mobile. |
| [prd-mvp.md](./prd-mvp.md) | Cahier des charges (PRD) du MVP : contexte, objectifs produit, périmètre fonctionnel et exigences. |
| [publication-ios.md](./publication-ios.md) | Procédure de publication iOS (signature, certificats, App Store Connect). |

Le sous-dossier [`_archive/`](./_archive/) conserve une ancienne « vision globale » désormais hors-sujet (`vision-globale-obsolete.md`), gardée pour mémoire uniquement — elle ne reflète plus l'architecture ni le périmètre actuels.

## Conventions

Chaque fichier commence par un titre et une ligne de statut datée : la documentation est un instantané, non un contrat figé. Lorsqu'un détail technique diverge du code, c'est le code qui fait foi ; signalez ou corrigez le document concerné plutôt que de le laisser dériver.
