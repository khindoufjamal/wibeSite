Voici le contexte détaillé de la mission, reconstitué à partir de ce que la recruteuse t'a communiqué et de tes notes.

Contexte métier

Tu rejoins une équipe de la FDJ qui développe et maintient une application critique transactionnelle utilisée par deux clients B2B.

L'application est en pleine phase de modernisation technique tout en continuant à évoluer fonctionnellement. C'est une application sensible où la disponibilité, les performances et la sécurité sont essentielles.

Elle participe également à la lutte contre la fraude, ce qui implique de fortes exigences sur la sécurité, l'authentification et la fiabilité des traitements.


---

Équipe

L'équipe est volontairement réduite :

1 Product Owner

1 Business Analyst

1 Technical Leader

3 Développeurs


Toute l'équipe est basée en France.

Cela signifie que chaque développeur est fortement impliqué dans les décisions techniques et le support.


---

Les enjeux actuels

1. Renfort de l'équipe

Deux nouveaux clients B2B arrivent.

L'augmentation du trafic et des demandes nécessite un développeur capable d'être rapidement autonome.


---

2. Support N3

C'est probablement LE point le plus important.

Ils cherchent quelqu'un capable de :

investiguer les incidents

analyser les logs

comprendre rapidement l'origine d'un problème

corriger les bugs

réaliser des hotfix

assurer les astreintes


Ils ont insisté sur août et septembre, période où une partie de l'équipe sera en congés.

Ils veulent quelqu'un de fiable qui puisse prendre le relais.


---

3. Migration technique

Un chantier important est en cours.

Migration Spring Boot → Quarkus

Objectifs :

meilleures performances

réduction de la consommation mémoire

démarrage plus rapide

meilleure intégration Kubernetes

architecture cloud native



---

Migration authentification

Aujourd'hui :

OIDC

OAuth2


Objectif :

Migration vers Keycloak comme Identity Provider.

Cela implique :

gestion des JWT

rôles

permissions

SSO

sécurisation des API



---

Architecture

L'architecture est hybride.

Ils possèdent :

un monolithe historique

plusieurs microservices


Les deux doivent continuer à communiquer.

Tu devras intervenir sur les deux parties.


---

Communication

Ils utilisent de la programmation asynchrone.

Tu peux t'attendre à :

Kafka

files de messages

événements

traitements asynchrones


L'objectif est de garantir des traitements transactionnels performants.


---

Performance

Ils ont insisté dessus.

Les sujets importants sont :

Hibernate

Ils attendent quelqu'un qui maîtrise :

cache de niveau 1

cache de niveau 2

optimisation des requêtes

N+1

Lazy/Eager Loading

Fetch Join



---

Base de données

PostgreSQL

MySQL


Optimisations :

index

requêtes SQL

plans d'exécution

transactions



---

Kubernetes

Applications conteneurisées.

Compétences attendues :

Docker

Kubernetes

déploiement

logs

monitoring



---

Développement

Ce n'est pas uniquement de la maintenance.

Tu développeras également :

nouvelles User Stories

évolutions fonctionnelles

refactoring

migration technique



---

Fonctionnel

Le développeur est impliqué dans :

compréhension du besoin métier

échanges avec le BA

proposition de solutions

estimation

développement

livraison



---

Domaine métier

Le système est fortement transactionnel.

Les priorités sont :

performance

disponibilité

sécurité

cohérence des transactions


En parallèle, il existe une forte dimension fraude, ce qui explique les exigences sur l'authentification, la sécurité et la traçabilité.


---

Pourquoi ton profil correspond

En t'appuyant sur ton CV, tu peux faire les liens suivants :

Société Générale : Java 21, Spring Boot, Quarkus, PostgreSQL, RabbitMQ, microservices, OAuth2, Azure, architecture moderne. 

BRED : Kafka, applications bancaires transactionnelles, résolution d'incidents de production, microservices, Spring Boot. 

IDEMIA : authentification forte, SAML, OAuth2, OpenID Connect, sécurité, lutte contre la fraude, optimisation des performances, support et maintenance. 


Au vu de ce contexte, l'entretien sera probablement orienté à 40 % sur la production (support N3 et investigation), 40 % sur l'architecture et la performance (Quarkus, Hibernate, Kubernetes, programmation asynchrone) et 20 % sur le métier et ton expérience des systèmes transactionnels. C'est sur ces sujets qu'il faut particulièrement te préparer.
