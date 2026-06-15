-- Suppression des anciennes donnees dans l'ordre des dependances
DELETE FROM signature;
DELETE FROM facture;
DELETE FROM livraison;
DELETE FROM chauffeur;
DELETE FROM client;
DELETE FROM users;

-- Insertion des utilisateurs (User)
INSERT INTO users (id, username, password, role) VALUES
  (1, 'chauffeur1', 'chauffeurpass', 'CHAUFFEUR'),
  (2, 'chauffeur2', 'chauffeurpass', 'CHAUFFEUR'),
  (3, 'client1', 'clientpass', 'CLIENT'),
  (4, 'client2', 'clientpass', 'CLIENT');

-- Insertion des details Chauffeur
INSERT INTO chauffeur (id, nom, prenom, email, telephone) VALUES
  (1, 'Dupont', 'Jean', 'jean.dupont@translog.com', '0600000001'),
  (2, 'Martin', 'Paul', 'paul.martin@translog.com', '0600000002');

-- Insertion des details Client
INSERT INTO client (id, nom, email, telephone, adresse) VALUES
  (3, 'Societe Alpha', 'contact@alpha.com', '0700000001', '10 avenue Alpha'),
  (4, 'Societe Beta', 'contact@beta.com', '0700000002', '20 avenue Beta');

INSERT INTO entreprise (id, nom, adresse, email, telephone) VALUES
  (1, 'TransLog', '1 rue de la Logistique', 'contact@translog.com', '0102030405');

INSERT INTO camion (id, immatriculation, marque, modele, capacite) VALUES
  (1, 'AB-123-CD', 'Renault', 'Premium', 20),
  (2, 'EF-456-GH', 'Mercedes', 'Actros', 25);

INSERT INTO livraison (id, reference, date_livraison, statut, chauffeur_id, camion_id, client_id) VALUES
  (1, 'LIV001', '2024-06-01', 'EN_COURS', 1, 1, 3),
  (2, 'LIV002', '2024-06-02', 'LIVREE', 2, 2, 4);

INSERT INTO facture (id, numero, montantht, montanttva, montantttc, livraison_id) VALUES
  (1, 'FAC001', 100.00, 20.00, 120.00, 1),
  (2, 'FAC002', 200.00, 40.00, 240.00, 2);

INSERT INTO signature (id, signataire, date_signature, type, livraison_id) VALUES
  (1, 'Jean Dupont', '2024-06-01T10:00:00', 'CHAUFFEUR', 1),
  (2, 'Client Alpha', '2024-06-01T11:00:00', 'CLIENT', 1),
  (3, 'Paul Martin', '2024-06-02T10:00:00', 'CHAUFFEUR', 2),
  (4, 'Client Beta', '2024-06-02T11:00:00', 'CLIENT', 2);
