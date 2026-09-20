-- Donnees de demonstration idempotentes. Elles ne suppriment pas les comptes crees via /api/auth/register.
-- Hibernate update ne remplace pas la contrainte enum deja presente dans PostgreSQL.
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'ENTREPRISE', 'CHAUFFEUR', 'CLIENT'));
-- Migration des mots de passe en clair utilises par les anciennes donnees de demonstration.
UPDATE users SET username = 'jean.dupont@translog.com', password = '$2a$10$FGkeX5Di4QmGu.SqOHLZuO3jJ5JfjoT7Z1K2HIoovYpbsh6YfAbi2', role = 'CHAUFFEUR'
WHERE id = 1 AND password = 'chauffeurpass';
UPDATE users SET username = 'paul.martin@translog.com', password = '$2a$10$FGkeX5Di4QmGu.SqOHLZuO3jJ5JfjoT7Z1K2HIoovYpbsh6YfAbi2', role = 'CHAUFFEUR'
WHERE id = 2 AND password = 'chauffeurpass';
UPDATE users SET username = 'contact@alpha.com', password = '$2a$10$ui3hVR5g.o.OJf0I6qZUJ./YfVZb1H.qG3QAePDHfwljDvEqrtDSK', role = 'CLIENT'
WHERE id = 3 AND password = 'clientpass';
UPDATE users SET username = 'contact@beta.com', password = '$2a$10$ui3hVR5g.o.OJf0I6qZUJ./YfVZb1H.qG3QAePDHfwljDvEqrtDSK', role = 'CLIENT'
WHERE id = 4 AND password = 'clientpass';

INSERT INTO users (id, username, password, role) VALUES
  (1, 'jean.dupont@translog.com', '$2a$10$FGkeX5Di4QmGu.SqOHLZuO3jJ5JfjoT7Z1K2HIoovYpbsh6YfAbi2', 'CHAUFFEUR'),
  (2, 'paul.martin@translog.com', '$2a$10$FGkeX5Di4QmGu.SqOHLZuO3jJ5JfjoT7Z1K2HIoovYpbsh6YfAbi2', 'CHAUFFEUR'),
  (3, 'contact@alpha.com', '$2a$10$ui3hVR5g.o.OJf0I6qZUJ./YfVZb1H.qG3QAePDHfwljDvEqrtDSK', 'CLIENT'),
  (4, 'contact@beta.com', '$2a$10$ui3hVR5g.o.OJf0I6qZUJ./YfVZb1H.qG3QAePDHfwljDvEqrtDSK', 'CLIENT'),
  (5, 'test.jwt2@tariki.ma', '$2a$10$6TjjY2MLWisWOhc3iqPFseWNe5VsXYW0RgBzPYIuayG5Y8gC3xFWO', 'CHAUFFEUR')
ON CONFLICT DO NOTHING;

-- Insertion des details Chauffeur
INSERT INTO chauffeur (id, nom, prenom, email, telephone) VALUES
  (1, 'Dupont', 'Jean', 'jean.dupont@translog.com', '0600000001'),
  (2, 'Martin', 'Paul', 'paul.martin@translog.com', '0600000002'),
  (5, 'JWT', 'Test', 'test.jwt2@tariki.ma', '0600000005')
ON CONFLICT DO NOTHING;

-- Insertion des details Client
INSERT INTO client (id, nom, email, telephone, adresse) VALUES
  (3, 'Societe Alpha', 'contact@alpha.com', '0700000001', '10 avenue Alpha'),
  (4, 'Societe Beta', 'contact@beta.com', '0700000002', '20 avenue Beta')
ON CONFLICT DO NOTHING;

INSERT INTO entreprise (id, nom, adresse, email, telephone) VALUES
  (1, 'TransLog', '1 rue de la Logistique', 'contact@translog.com', '0102030405')
ON CONFLICT DO NOTHING;

INSERT INTO camion (id, immatriculation, marque, modele, capacite) VALUES
  (1, 'AB-123-CD', 'Renault', 'Premium', 20),
  (2, 'EF-456-GH', 'Mercedes', 'Actros', 25)
ON CONFLICT DO NOTHING;

INSERT INTO livraison (id, reference, date_livraison, statut, chauffeur_id, camion_id, client_id) VALUES
  (1, 'LIV001', '2024-06-01', 'EN_COURS', 1, 1, 3),
  (2, 'LIV002', '2024-06-02', 'LIVREE', 2, 2, 4)
ON CONFLICT DO NOTHING;

INSERT INTO facture (id, numero, montantht, montanttva, montantttc, livraison_id) VALUES
  (1, 'FAC001', 100.00, 20.00, 120.00, 1),
  (2, 'FAC002', 200.00, 40.00, 240.00, 2)
ON CONFLICT DO NOTHING;

INSERT INTO signature (id, signataire, date_signature, type, livraison_id) VALUES
  (1, 'Jean Dupont', '2024-06-01T10:00:00', 'CHAUFFEUR', 1),
  (2, 'Client Alpha', '2024-06-01T11:00:00', 'CLIENT', 1),
  (3, 'Paul Martin', '2024-06-02T10:00:00', 'CHAUFFEUR', 2),
  (4, 'Client Beta', '2024-06-02T11:00:00', 'CLIENT', 2)
ON CONFLICT DO NOTHING;

SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT COALESCE(MAX(id), 1) FROM users), true);
SELECT setval(pg_get_serial_sequence('entreprise', 'id'), (SELECT COALESCE(MAX(id), 1) FROM entreprise), true);
SELECT setval(pg_get_serial_sequence('camion', 'id'), (SELECT COALESCE(MAX(id), 1) FROM camion), true);
SELECT setval(pg_get_serial_sequence('livraison', 'id'), (SELECT COALESCE(MAX(id), 1) FROM livraison), true);
SELECT setval(pg_get_serial_sequence('facture', 'id'), (SELECT COALESCE(MAX(id), 1) FROM facture), true);
SELECT setval(pg_get_serial_sequence('signature', 'id'), (SELECT COALESCE(MAX(id), 1) FROM signature), true);
