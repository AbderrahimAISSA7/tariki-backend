# Messagerie

## Rattachements

- Entreprise / chauffeur : une conversation privee par responsable et chauffeur de la meme entreprise.
- Client / chauffeur : une conversation par livraison, uniquement entre son client et son chauffeur affecte.
- L'entreprise (et le profil ADMIN) ne peut pas consulter les conversations client / chauffeur via l'API.
- Les contacts sont derives du serveur : pas de recherche libre de comptes ni de destinataire arbitraire.
- Les livraisons programmees et terminees gardent leur conversation tant que l'affectation reste valide.
- Un changement d'affectation invalide l'ancien acces. Le remplacant n'herite pas de l'historique prive.
- Le retrait d'un chauffeur de son entreprise coupe l'acces aux conversations internes concernees.

## Chiffrement

Le corps des messages est chiffre AVANT insertion dans PostgreSQL avec AES-256-GCM.
Chaque message utilise un nonce aleatoire de 12 octets et un tag d'authentification de 128 bits.
Le canal, l'expediteur et l'identifiant logique du message sont authentifies comme donnees associees.
Les identifiants, horodatages et accuses de lecture restent des metadonnees non chiffrees.
Ni le corps en clair ni un apercu en clair ne sont enregistres dans une autre colonne.

La cle est distincte de la cle JWT. Le serveur refuse de demarrer si elle manque ou n'a pas 32 octets.
Il s'agit d'un chiffrement au stockage, PAS de bout en bout : le backend dechiffre pour les participants autorises.
Utiliser HTTPS en dehors de localhost pour proteger le transport. L'environnement local utilise HTTP.

Initialisation locale, depuis `tariki-backend` :

```powershell
./scripts/init-chat-key.ps1
docker compose up -d --build backend
```

La cle aleatoire est conservee dans `.env`, ignore par Git et Docker build. Le script ne remplace pas une cle existante.
Compose transmet `CHAT_ENCRYPTION_KEY` au backend. Pour Maven, exporter cette meme variable avant le demarrage.
Ne jamais versionner, afficher dans les logs ou transmettre cette cle au frontend.

En production, injecter la cle depuis un gestionnaire de secrets, restreindre les acces aux conteneurs et utiliser TLS.
Sauvegarder la cle SEPAREMENT des sauvegardes PostgreSQL : la perdre rend l'historique indechiffrable.
Ne pas regenerer/remplacer la cle sur une base existante : la rotation necessite une migration de rechiffrement,
qui n'est pas implementee ici. Les sauvegardes contenant `chat_message` conservent les corps chiffres.

## Temps reel

POST REST pour les envois, SSE pour notifier uniquement les deux participants apres validation de la transaction.
Le flux SSE n'emporte ni texte ni identifiant de conversation : chaque lecture REST revalide le rattachement.
JWT dans l'en-tete Authorization, jamais dans l'URL. Le flux est renouvele au plus tard toutes les 55 secondes
et ferme a l'expiration du JWT. Reconnexion automatique et rechargement de l'historique apres une coupure.
Les messages sont limites a 4000 caracteres, les pages d'historique a 50 messages, et les flux a 10 par compte.
Un identifiant d'envoi stable permet de retenter un envoi sans doublon. Les brouillons restent en memoire du navigateur.

Le diffuseur SSE est local a UNE instance backend (le Compose actuel). Pour plusieurs replicas, ajouter un bus
de notifications partage (Redis, etc.). Desactiver le buffering du reverse proxy sur `/api/chat/events`.
`spring.jpa.open-in-view=false` evite de retenir une connexion PostgreSQL pendant la duree d'un flux SSE.
Il n'y a pas de pieces jointes, de notification systeme, de presence utilisateur ou de politique de purge automatique.

## Essais

Comptes : voir [DEMO.md](DEMO.md). Ouvrir deux profils dans des navigateurs ou sessions privees distincts.
Les onglets sont `Communication interne` (entreprise/chauffeur) et `Messages prives` (client/chauffeur).
Le detail d'une livraison donne un acces direct a sa conversation privee.

```powershell
# Backend : H2 isole, avec une cle de test uniquement dans src/test/resources
mvn test

# Frontend : API active et frontend sur 5175
npm run test:e2e
```

Les tests navigateur envoient des messages marques `[Test messagerie ...]` aux comptes de demonstration.
Ils ne modifient pas les affectations ni les statuts des livraisons.

References techniques : [Java Cipher / GCM](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/Cipher.html),
[Spring SSE](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-async.html),
[Microsoft fetch-event-source](https://github.com/Azure/fetch-event-source).
