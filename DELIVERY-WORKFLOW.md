# Livraison, invitation et facture

## Parcours

1. L'entreprise complete ses coordonnees et son logo dans **Entreprise**. ICE, IF et RC sont facultatifs. Le logo PNG/JPEG est limite a 1 Mo et 2000 x 1200 pixels ; sans logo, la facture affiche la raison sociale.
2. **Nouvelle livraison** : choisir un client rattache ou renseigner un nouveau client (email, nom, prenom, telephone, adresse). Saisir le service, le prix HT en MAD et le taux de TVA. Aucun taux n'est presume. La reference est generee si elle est laissee vide.
3. La fiche affiche un QR code et un lien client. L'invitation est valable 7 jours ; son renouvellement invalide l'ancien lien. Un compte existant est dirige vers sa livraison apres connexion.
4. Le nouveau client ouvre son invitation sur son telephone et choisit son mot de passe. Son role, son email d'invitation et son rattachement ne peuvent pas etre changes par la requete d'inscription.
5. Le chauffeur demarre puis clique **Terminer la livraison** a l'arrivee. Le statut devient `EN_ATTENTE_VALIDATION`. Le GPS cesse d'etre partage en direct ; la derniere position connue reste conservee selon les regles de suivi.
6. Seul le client affecte, connecte a son compte, peut signer et confirmer la reception. L'interface chauffeur ne permet pas de signer a sa place. Le serveur controle l'affectation, le statut, le tarif et sa version, le consentement et le PNG signe.
7. La transaction archive la signature, son compte signataire et son horodatage, cloture la livraison et genere le PDF. Une double validation ne genere pas de seconde facture.
8. **Livraisons > Terminee** et **Factures** permettent au client et a l'entreprise de telecharger le PDF. Le chauffeur voit l'etat de reception, sans acces aux factures.

## Conservation

- Le prix HT et le taux sont decimaux ; la TVA est arrondie au centime avec `HALF_UP`, le TTC est HT + TVA.
- Numero `FAC-annee-identifiant` unique pour les factures generees. Ce format n'est pas une promesse de numerotation fiscale sans rupture.
- PDF en `facture.pdf` (`bytea`), instantane textuel de facturation, signature PNG en `signature.image_png`, compte signataire et date UTC. Les dates du PDF sont affichees en heure `Africa/Casablanca`.
- Le PDF contient le logo de l'entreprise, ses coordonnees, celles du client, le trajet, le chargement, le service, les montants, la reception et sa signature. Il est fige : les changements ulterieurs de coordonnees n'alterent pas les factures emises.
- Modification et suppression des factures emises interdites par API. Un futur avoir doit etre implemente separement, pas en reecrivant une facture.
- Les anciennes livraisons terminees ne recoivent ni fausse signature ni facture retroactive. Un ancien document sans PDF affiche `PDF non archive`.
- Les anciens tarifs absents se completent sur la fiche, par l'entreprise, avant la validation du client.

## Securite Et Deploiement

- Une invitation est un secret d'activation : la transmettre uniquement au client concerne. Elle ne verifie pas l'identite civile ni la possession de l'adresse email. Le compte ne peut pas etre reactive/reinitialise avec un ancien lien.
- La cle de signature des invitations est derivee de `app.jwt.secret` avec un domaine distinct. Une invitation n'est jamais acceptee comme JWT d'acces. Definir un `JWT_SECRET` fort et stable en production ; son changement invalide les sessions et invitations.
- Utiliser HTTPS et une URL publique joignable depuis le telephone. Cote frontend : `VITE_PUBLIC_APP_URL=https://votre-domaine`. Sans ce parametre, le QR code utilise l'origine courante ; `localhost` n'est pas accessible depuis un autre telephone.
- Ne pas journaliser les chemins d'invitation ou les corps contenant mot de passe/signature dans les reverse proxies, outils de monitoring et traces. Les reponses d'invitation et PDF sont `no-store`.
- La signature est une trace manuscrite recueillie dans une session authentifiee, pas une signature cryptographique qualifiee ni une verification que l'appareil appartient au client. La conformite juridique et fiscale doit etre validee pour votre usage avant production.
- La police PDF embarquee couvre les noms en alphabet latin et accents. Les caracteres non pris en charge sont refuses sans cloturer la livraison ; une transcription latine est actuellement necessaire. L'ecriture arabe et sa mise en forme demandent une prise en charge complementaire.
- PDF et signatures sont proteges par les droits applicatifs, pas par un chiffrement applicatif au repos. Prevoir chiffrement des volumes/sauvegardes, politique de conservation, droits d'administration et sauvegardes. Le chiffrement AES-GCM du chat reste independant.
- Les colonnes sont ajoutees par Hibernate `ddl-auto=update` dans l'environnement local. En production, preparer une migration controlee et une sauvegarde avant deploiement.

## Verification

`mvn test` couvre roles, rattachements, renouvellement/expiration d'invitation, absence de detournement des JWT, activation unique, signature vide, consentement, version du tarif, arrondis, logo et PDF immuable.

Dans `tariki-frontend`, `npm run test:e2e -- tests/delivery-workflow.spec.ts` exerce le parcours navigateur desktop/mobile contre le backend Docker local. Il cree ses propres chauffeur/camion/client/livraison et supprime uniquement ces donnees temporaires. Les factures des utilisateurs ne sont pas modifiees.
