#!/bin/sh

# Faire échouer le script si une commande échoue
set -e

echo "🚀 Début du script ci_post_clone.sh"

# 1. Installer Java (nécessaire pour que Gradle compile ton code Kotlin)
echo "☕️ Installation de Java (OpenJDK 17)..."
brew install openjdk@17

# 2. Configurer JAVA_HOME pour Xcode Cloud (qui tourne sur des Mac Apple Silicon M-series)
export JAVA_HOME=$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home

# 3. Se placer à la racine du projet pour donner les droits d'exécution à Gradle
# On remonte de deux dossiers car on est dans iosApp/ci_scripts/
cd ../../

echo "🔧 Ajout des droits d'exécution au wrapper Gradle..."
chmod +x gradlew

echo "✅ Fin du script ci_post_clone.sh"