# 🚀 Tariki Backend

Backend principal de l'application **Tariki**, développé avec Spring Boot.

## 🎯 Objectif
Gérer toute la logique métier :
- Livraisons
- Chauffeurs
- Camions
- Facturation
- Signatures

---

## 🚀 Démarrage rapide

### Prérequis
- Java 17+
- Maven
- Docker

### Lancer en local

```powershell
# Initialiser la cle de messagerie locale (conserve une cle existante)
./scripts/init-chat-key.ps1

# Lancer PostgreSQL et le backend
docker compose up -d --build db backend
```

Le backend exige `CHAT_ENCRYPTION_KEY`, injectée par Compose depuis le fichier `.env`
non versionné. Pour lancer avec Maven, exporter cette même variable et libérer le port 8080.
Conserver la clé séparément des sauvegardes de la base.
Voir [CHAT.md](CHAT.md) pour la messagerie chiffrée et [DEMO.md](DEMO.md) pour les comptes de test.

### Documentation API

Une fois l'application démarrée, accédez à la documentation Swagger :

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🧠 Architecture

Controller → Service → Repository → Database

### Structure du projet

src/
├── controller/ # API REST
├── service/ # logique métier
├── repository/ # accès DB
├── model/ # entités
├── dto/ # objets d'échange
├── mapper/ # conversion DTO ↔ Entity
├── config/ # configuration
├── exception/ # gestion erreurs
└── util/ # helpers

---

## 🗄️ Modèle métier

### Entités principales

- Entreprise
- Chauffeur
- Camion
- Livraison
- Client
- Facture
- Signature

---

## 🔗 Relations


Entreprise
├── Chauffeur ─── Camion
│ │
│ └── Livraison ─── Client
│ │
│ ├── Facture
│ └── Signature


---

## 🔁 Workflow métier


Créer Livraison
↓
Assigner Chauffeur + Camion
↓
Chauffeur démarre
↓
Livraison en cours
↓
Signature Chauffeur + Client
↓
Génération Facture


---

## ⚙️ Fonctionnalités

- CRUD Chauffeurs
- CRUD Camions
- CRUD Livraisons
- Assignation Chauffeur/Camion
- Gestion des statuts
- Signature digitale
- Génération de factures

---

## 🧾 Facturation

- Calcul automatique :
  - HT
  - TVA
  - TTC
- Génération après validation livraison

---

## 🔐 Rôles (à venir)

- Admin (entreprise)
- Chauffeur
- Client

---

## 🐳 Docker

Le backend est conçu pour être lancé via Docker.

---

## 🚀 Objectif MVP

Un backend simple, robuste et scalable pour :
- gérer les livraisons
- garantir la traçabilité
- automatiser la facturation
