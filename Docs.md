Oui, là je ferais une section Troubleshooting dédiée, avant le problème du env/<username>.env. D’après ta capture du Dockerfile-base, le problème visé est bien l’échec de apt-get derrière Zscaler, car le certificat Zscaler n’est pas reconnu dans l’image Docker.

Tu peux ajouter ceci au README :

* apt-get update / apt-get install fails due to Zscaler certificate
If `apt-get update` or `apt-get install` fails while building the RTL Docker
image because of an SSL/certificate error, the Zscaler root certificate may
be missing from the Docker image.
Retrieve the Zscaler root CA certificate and save it as:
zscaler-root-ca.pem
Then update `Dockerfile-base` to copy the certificate into the container and
register it as a trusted CA:
COPY zscaler-root-ca.pem /usr/local/share/ca-certificates/zscaler-root-ca.crt
RUN update-ca-certificates
The package installation can then be executed normally:
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl tar wget vim && \
    rm -rf /var/lib/apt/lists/*

Puis j’ajouterais une précision importante :

The Zscaler certificate must be available in the Docker build context before
building the image.
After updating `Dockerfile-base`, rebuild the Docker image.

Et juste après, on peut mettre ton deuxième cas :

* Missing user-specific environment configuration
If you still encounter issues when building or starting the RTL stack, check
that your user-specific environment file exists:
env/<username>.env
If it does not exist yet, create it and configure the required local values,
especially `DEV_RTL_DOCKER_DIR`, `DEV_RTL_SRC_DIR`, `RTL_VERSION` and
`RTL_REPO`.

Je préfère cette formulation à « désactiver la vérification SSL » avec --no-check-certificate : installer le certificat Zscaler dans le trust store du conteneur est la solution propre. Sur ta capture, c’est d’ailleurs exactement la modification faite dans Dockerfile-base avec COPY zscaler-root-ca.pem ... puis update-ca-certificates.
