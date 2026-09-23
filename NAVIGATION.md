# Guidage des livraisons

## Parcours

L'entreprise renseigne une adresse de livraison et un point exact sur la carte
(clic, deplacement du repere ou latitude/longitude), a la creation ou sur une livraison
programmee/en cours. L'adresse de facturation du client reste independante.
Les anciennes livraisons restent valides mais le guidage exige un point renseigne.
Aucune destination approximative au centre d'une ville n'est ajoutee automatiquement.

Le chauffeur affecte demarre la livraison, puis accepte le GPS, le partage et le
calcul du trajet. Le guidage est aussi accessible depuis une livraison deja en cours.
La carte affiche la position locale, le trace, la prochaine instruction, la distance
et une duree restante estimee hors trafic. Les indications vocales sont optionnelles.
Le recalcul intervient en cas d'ecart, pas a chaque position. Un signal de plus de
30 secondes ou d'une precision superieure a 100 metres suspend les instructions.
Trois echecs de calcul consecutifs suspendent les tentatives automatiques ; le bouton
de recalcul reste disponible. L'arrivee a proximite ne termine jamais la livraison :
le chauffeur signale toujours son arrivee, puis le client valide la reception.

## Fournisseur Et Limites

Pour les premiers tests, le backend utilise OSRM/FOSSGIS :
`https://routing.openstreetmap.de/routed-car`.
Ce routage automobile standard ne gere PAS les restrictions poids lourds
(hauteur, tonnage, interdictions), ni le trafic en direct. Ce n'est pas un GPS
poids lourds certifie. Les consignes et panneaux routiers priment.

Le service public est sans garantie de disponibilite, avec un maximum d'une
requete par seconde et sans usage intensif. Le backend limite les appels a un par
1,1 seconde pour toute l'instance et un par 10 secondes par chauffeur. Le navigateur
attend au moins 15 secondes entre deux calculs, 30 secondes apres un echec.
Pour plusieurs instances backend ou un usage en production, utiliser une instance
OSRM auto-hebergee ou un fournisseur adapte, et un limiteur partage.

- Politique du fournisseur : https://routing.openstreetmap.de/about.html
- Protocole OSRM : https://project-osrm.org/docs/v5.24.0/api/
- `NAVIGATION_OSRM_URL` : base du serveur OSRM, configurable dans l'environnement Compose.
- `NAVIGATION_USER_AGENT` : identifiant de l'application (ajouter un contact en production).

Pas de geocodage automatique : l'adresse privee n'est pas envoyee a un moteur de
recherche. La selection du point sur la carte reste explicite. Le service Nominatim
public n'est pas utilise pour cette application de suivi.
Les seules donnees envoyees au serveur OSRM sont les coordonnees GPS de depart
et de destination, apres accord du chauffeur. Aucun JWT, nom, adresse ou identifiant
de livraison n'est transmis. Le fournisseur peut journaliser ces coordonnees.

## API Et Stockage

- `PATCH /api/livraisons/{id}/destination` : entreprise rattachee/admin uniquement,
  corps `adresseLivraison`, `destinationLatitude`, `destinationLongitude`, `version`.
  Controle de concurrence et refus apres l'arrivee.
- `POST /api/livraisons/{id}/navigation/route` : chauffeur affecte a une livraison
  active dans la meme entreprise uniquement. Corps `latitude`, `longitude`,
  `accuracy`, `observedAt`. La destination est lue en base, jamais imposee par le chauffeur.
- Le trajet reste en memoire dans le navigateur. La derniere position partagee
  conserve le fonctionnement et les restrictions d'acces du suivi GPS existant.
- Les trois nouvelles colonnes de `livraison` sont nullables. En developpement,
  Hibernate `ddl-auto=update` les ajoute sans modifier les donnees existantes.
  En production avec migrations gerees, ajouter `adresse_livraison varchar(500)`,
  `destination_latitude double precision`, `destination_longitude double precision`.

## Telephone

HTTPS (ou localhost sur l'appareil) et autorisation GPS sont obligatoires.
Le mode agrandi garde la carte visible ; Screen Wake Lock est demande si disponible.
Un navigateur mobile ne garantit pas le GPS, la voix ou le reseau en arriere-plan
ou ecran verrouille. Un rechargement exige une nouvelle activation volontaire.
Arreter le guidage ne coupe pas le partage deja consenti ; le bouton GPS l'arrete.
Terminer la livraison coupe le partage et retire le guidage.
