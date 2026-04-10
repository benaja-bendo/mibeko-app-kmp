# Mibeko - Lois de la République du Congo 🇨🇬

[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.5.11-blue.svg?logo=jetpack-compose)](https://github.com/JetBrains/compose-multiplatform)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Mibeko** est une application mobile moderne conçue pour faciliter l'accès aux textes législatifs et réglementaires de la **République du Congo**. Développée avec **Kotlin Multiplatform (KMP)** et **Compose Multiplatform**, elle offre une expérience fluide sur Android et iOS.

## ✨ Fonctionnalités

- 📚 **Exploration :** Parcourez les différents codes (Code Civil, Code Pénal, etc.) et lois de la République.
- 🔍 **Recherche Avancée :** Trouvez rapidement un article ou un mot-clé spécifique.
- ⭐ **Favoris :** Enregistrez les articles importants pour une consultation hors-ligne.
- 🌙 **Mode Sombre :** Interface adaptée pour une lecture confortable de jour comme de nuit.
- 📱 **Multiplateforme :** Code partagé entre Android et iOS pour une maintenance simplifiée.

## 🛠 Architecture

Le projet utilise les dernières technologies de l'écosystème Kotlin :
- **Compose Multiplatform** pour l'interface utilisateur partagée.
- **Koin** ou **Voyager** (à confirmer selon l'implémentation) pour l'injection et la navigation.
- **Material 3** pour un design propre et moderne.

## 🚀 Installation & Build

### Prérequis
- Android Studio Hedgehog ou supérieur.
- Xcode 15+ (pour la partie iOS).
- JDK 17.

### Cloner le projet
```bash
git clone https://github.com/votre-user/mibeko-congo.git
cd mibeko-congo
```

### Build Android
```bash
./gradlew :composeApp:assembleDebug
```

### Build iOS
Ouvrez le dossier `iosApp` dans Xcode ou utilisez la configuration de run dans Android Studio.

## 🤝 Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou à soumettre une pull request.

## ⚖️ Licence

Ce projet est sous licence **MIT**. Voir le fichier [LICENSE](LICENSE) pour plus de détails.
---
*Conçu avec ❤️ pour faciliter l'accès au droit au Congo.*
