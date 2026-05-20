-- Suppression des anciennes insertions pour Chauffeur et Client
DELETE FROM chauffeur;
DELETE FROM client;
DELETE FROM "user";

-- Insertion des utilisateurs (User)
INSERT INTO "user" (id, username, password, role) VALUES
  (1, 'admin', 'adminpass', 'ADMIN'),
  (2, 'chauffeur1', 'chauffeurpass', 'CHAUFFEUR'),
  (3, 'client1', 'clientpass', 'CLIENT');

-- Insertion des détails Chauffeur
INSERT INTO chauffeur (id, nom, prenom, email, telephone) VALUES
  (2, 'Dupont', 'Jean', 'jean.dupont@translog.com', '0600000001');

-- Insertion des détails Client
INSERT INTO client (id, nom, email, telephone, adresse) VALUES
  (3, 'Société Alpha', 'contact@alpha.com', '0700000001', '10 avenue Alpha');

INSERT INTO entreprise (id, nom, adresse, email, telephone) VALUES
  (1, 'TransLog', '1 rue de la Logistique', 'contact@translog.com', '0102030405');

INSERT INTO camion (id, immatriculation, marque, modele, capacite) VALUES
  (1, 'AB-123-CD', 'Renault', 'Premium', 20),
  (2, 'EF-456-GH', 'Mercedes', 'Actros', 25);

INSERT INTO livraison (id, reference, date_livraison, statut, chauffeur_id, camion_id, client_id) VALUES
  (1, 'LIV001', '2024-06-01', 'EN_COURS', 1, 1, 1),
  (2, 'LIV002', '2024-06-02', 'LIVREE', 2, 2, 2);

INSERT INTO facture (id, numero, montantht, montanttva, montantttc, livraison_id) VALUES
  (1, 'FAC001', 100.00, 20.00, 120.00, 1),
  (2, 'FAC002', 200.00, 40.00, 240.00, 2);

INSERT INTO signature (id, signataire, date_signature, type, livraison_id) VALUES
  (1, 'Jean Dupont', '2024-06-01T10:00:00', 'CHAUFFEUR', 1),
  (2, 'Client Alpha', '2024-06-01T11:00:00', 'CLIENT', 1),
  (3, 'Paul Martin', '2024-06-02T10:00:00', 'CHAUFFEUR', 2),
  (4, 'Client Beta', '2024-06-02T11:00:00', 'CLIENT', 2);
