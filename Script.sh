#!/bin/bash

# ==========================
# Variables de configuration
# ==========================
IMAGE_NAME="sabr_httpd_proxy:1.0.0"
CONTAINER_NAME="sabr_httpd_proxy"
CONF_FILE="ihm_vhost.conf"
DEST_PATH="/etc/httpd/conf/ihm_vhost.conf"
PORTS="-p 5443:443/tcp"

# ==========================
# Vérifications initiales
# ==========================
echo "🔍 Vérification du fichier de configuration..."
if [ ! -f "$CONF_FILE" ]; then
  echo "❌ Fichier $CONF_FILE introuvable dans le répertoire courant."
  exit 1
fi

# ==========================
# Lancement du conteneur
# ==========================
echo "🚀 Lancement du conteneur à partir de l'image $IMAGE_NAME ..."
docker run -d $PORTS --name $CONTAINER_NAME $IMAGE_NAME

if [ $? -ne 0 ]; then
  echo "❌ Échec du lancement du conteneur. Vérifie que l'image existe et que le port 5443 n'est pas déjà utilisé."
  exit 1
fi

# Récupération de l'ID du conteneur
CONTAINER_ID=$(docker ps -aqf "name=$CONTAINER_NAME")
echo "🆔 Conteneur lancé avec l'ID : $CONTAINER_ID"

# ==========================
# Copie du fichier de configuration
# ==========================
echo "📁 Copie du fichier $CONF_FILE vers $DEST_PATH dans le conteneur..."
docker cp "$CONF_FILE" "$CONTAINER_ID:$DEST_PATH"

if [ $? -ne 0 ]; then
  echo "❌ Échec de la copie du fichier de configuration."
  exit 1
fi

# ==========================
# Redémarrage gracieux d'Apache
# ==========================
echo "🔁 Redémarrage du service httpd dans le conteneur..."
docker exec -it "$CONTAINER_ID" bash -c "/usr/sbin/apachectl -k graceful"

if [ $? -eq 0 ]; then
  echo "✅ Apache redémarré avec succès !"
  echo "🌐 Le proxy est accessible sur https://localhost:5443"
else
  echo "⚠️ Problème lors du redémarrage du service Apache."
fi
