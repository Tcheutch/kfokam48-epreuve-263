-- ---------------------------------------------------------------------------
-- Jeu de démonstration, chargé au démarrage de l'application (ENF6).
--
-- Il vit dans un emplacement Flyway SÉPARÉ de db/migration : les tests ne le
-- chargent pas, pour qu'ils partent d'une base vide et restent lisibles.
-- Voir spring.flyway.locations dans application.yml.
-- ---------------------------------------------------------------------------

INSERT INTO promotion (id, nom) VALUES
    (1, 'Promotion Java 2026 — Yaoundé'),
    (2, 'Promotion Web 2026 — Douala');

INSERT INTO utilisateur (id, nom, role, promotion_id) VALUES
    (1,  'Madame Ngo Bell',   'FORMATEUR', 1),
    (2,  'Monsieur Essomba',  'FORMATEUR', 2),
    (10, 'Awa Ndiaye',        'ETUDIANT',  1),
    (11, 'Bilal Moussa',      'ETUDIANT',  1),
    (12, 'Chantal Fotso',     'ETUDIANT',  1),
    (13, 'Diane Mbarga',      'ETUDIANT',  1),
    (14, 'Emeka Okafor',      'ETUDIANT',  1),
    (15, 'Fatou Sarr',        'ETUDIANT',  1),
    (20, 'Grace Ateba',       'ETUDIANT',  2),
    (21, 'Hamed Traoré',      'ETUDIANT',  2);

-- Les séquences d'identité doivent repartir après les identifiants posés à la main.
ALTER TABLE promotion   ALTER COLUMN id RESTART WITH 100;
ALTER TABLE utilisateur ALTER COLUMN id RESTART WITH 100;
