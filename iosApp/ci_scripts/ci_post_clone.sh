#!/bin/sh

# Faire échouer le script si une commande échoue
set -e

echo "🚀 Début du script ci_post_clone.sh"

# 1. Installer Java (nécessaire pour que Gradle compile ton code Kotlin)
echo "☕️ Installation de Java (OpenJDK 17)..."
brew install openjdk@17

# 2. Exporter JAVA_HOME de manière persistante pour Xcode Cloud
# Au lieu d'utiliser sudo (qui est bloqué), on exporte simplement le chemin.
export JAVA_HOME=$(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

# 3. Se placer à la racine du projet pour donner les droits d'exécution à Gradle
# On remonte de deux dossiers car on est dans iosApp/ci_scripts/
cd ../../

echo "🔧 Ajout des droits d'exécution au wrapper Gradle..."
chmod +x gradlew

echo "✅ Fin du script ci_post_clone.sh"