#!/bin/sh

# Faire échouer le script si une commande échoue
set -e

echo "🚀 Début du script ci_post_clone.sh"

# 1. Installer Java (nécessaire pour que Gradle compile ton code Kotlin)
echo "☕️ Installation de Java (OpenJDK 17)..."
brew install openjdk@17

# 2. Configurer JAVA_HOME de manière globale pour que Xcode le trouve pendant le build
# Xcode Cloud tourne sous macOS, l'emplacement par défaut de brew est /usr/local sur Intel ou /opt/homebrew sur Silicon.
# On ajoute un lien symbolique pour que le système le reconnaisse nativement.
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Exporter pour le script courant
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 3. Se placer à la racine du projet pour donner les droits d'exécution à Gradle
# On remonte de deux dossiers car on est dans iosApp/ci_scripts/
cd ../../

echo "🔧 Ajout des droits d'exécution au wrapper Gradle..."
chmod +x gradlew

echo "✅ Fin du script ci_post_clone.sh"