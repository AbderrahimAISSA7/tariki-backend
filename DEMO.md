# Comptes de demonstration

Les comptes sont crees avec le profil Spring `demo`, active dans le Compose local.
Les mots de passe sont hashes avec BCrypt.

| Profil | Email | Mot de passe |
| --- | --- | --- |
| Entreprise | entreprise.demo@tariki.ma | Entreprise123! |
| Chauffeur | chauffeur.demo@tariki.ma | Chauffeur123! |
| Client | client.demo@tariki.ma | Client123! |

## Donnees initiales

- Oriental Transport (Oujda) : trois chauffeurs, trois camions et sept livraisons.
- Entreprise : deux livraisons en cours, trois terminees et deux programmees.
- Youssef El Amrani : une livraison en cours, deux terminees et deux programmees.
- Figuig Materiaux : OT-2026-001, 10 tonnes de ciment d'Oujda vers Figuig.
- La derniere position declaree de cette livraison est Bouarfa. Il s'agit d'une
  donnee de demonstration, pas d'un suivi GPS.
- Les dates sont relatives au jour de la premiere initialisation.

L'initialisation est idempotente : redemarrer ne reinitialise ni les mots de passe
ni les statuts modifies. Les comptes existants et les anciennes donnees sont conserves.

## Demarrage

Dans `tariki-backend` :

```powershell
./scripts/init-chat-key.ps1
docker compose up -d --build db backend
```

Alternative Maven (arreter d'abord le conteneur backend pour liberer 8080) :

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Dans `tariki-frontend` :

```powershell
npm run dev
```

Se connecter successivement avec les trois comptes, en se deconnectant entre les essais.
Le bouton de deconnexion est en bas du menu lateral.

## Comportements a verifier

- L'entreprise voit uniquement sa flotte, ses clients et ses livraisons.
- Le chauffeur voit ses missions et peut les demarrer puis les terminer.
- Une mission ne peut pas demarrer si son chauffeur ou son camion est deja en livraison.
- Le client consulte seulement ses propres livraisons. Les changements de statut
  apparaissent dans le detail a l'actualisation ou sous 30 secondes.
- Le client et le chauffeur n'ont pas les menus Camions / Chauffeurs. Les URL directes
  sont redirigees et les endpoints de gestion sont refuses par le serveur.
- Un identifiant de livraison d'un autre compte ne donne pas acces a ses donnees.
- Les factures et signatures suivent les memes restrictions de propriete.
- Aucun jeu de donnees fictif n'est affiche si le backend est indisponible.

## Verification

```powershell
# Backend : tests isoles en H2, sans toucher a PostgreSQL
mvn test

# Frontend : API et frontend actifs ; port par defaut 5175
npm run build
npx playwright test
```

La variable `TARIKI_TEST_URL` permet de choisir le port frontend des tests.
Les tests navigateur n'alterent pas les livraisons ; les transitions sont testees en H2.
Pour un deploiement hors demonstration, desactiver le profil `demo` et definir `JWT_SECRET`.

La messagerie necessite aussi `CHAT_ENCRYPTION_KEY`. Voir [CHAT.md](CHAT.md) pour les acces,
le chiffrement, la sauvegarde de la cle et les essais en direct.
