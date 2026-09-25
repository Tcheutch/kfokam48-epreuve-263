-- ---------------------------------------------------------------------------
-- V901 — Séances, présences, exercices et relectures de démonstration.
--
-- Migration SÉPARÉE de V900, qui est déjà livrée : on ne modifie jamais une
-- migration appliquée, on en ajoute une (contrainte B5).
--
-- Ce jeu poursuit deux buts :
--   1. l'application n'est jamais vide au premier écran (ENF6) ;
--   2. CHAQUE code d'erreur du contrat est reproductible avec des identifiants
--      fixes — voir docs/ERREURS.md. Sans une séance déjà expirée, le
--      410 CODE_EXPIRE serait invérifiable sans attendre quinze minutes.
--
-- SQL PostgreSQL : cet emplacement n'est jamais chargé par les tests.
-- ---------------------------------------------------------------------------

INSERT INTO session (id, titre, code, ouverture_at, expiration_at, cloture_at, promotion_id, formateur_id) VALUES
    -- Séance OUVERTE : le code marche, tout est permis.
    (1000, 'Spring Boot — jour 4', 'DEMO01',
     (NOW() AT TIME ZONE 'UTC'),
     (NOW() AT TIME ZONE 'UTC') + INTERVAL '15 minutes',
     NULL, 1, 1),

    -- Séance EXPIREE : ouverte il y a 16 minutes. Le code ne marche plus
    -- (RG2 -> 410), mais les dépôts restent possibles (RG12 -> 201).
    -- C'est cette ligne qui rend le 410 reproductible sans attendre.
    (1001, 'React — jour 3', 'DEMO02',
     (NOW() AT TIME ZONE 'UTC') - INTERVAL '16 minutes',
     (NOW() AT TIME ZONE 'UTC') - INTERVAL '1 minute',
     NULL, 1, 1),

    -- Séance CLOTUREE : plus rien n'est permis (RG20 -> 409).
    (1002, 'SQL — jour 2', 'DEMO03',
     (NOW() AT TIME ZONE 'UTC') - INTERVAL '3 hours',
     (NOW() AT TIME ZONE 'UTC') - INTERVAL '2 hours 45 minutes',
     (NOW() AT TIME ZONE 'UTC') - INTERVAL '1 hour', 1, 1);

-- Trois présents à la séance ouverte, dont une ajoutée à la main par le
-- formateur : le tableau doit la distinguer (RG14, Q14).
INSERT INTO presence (id, session_id, etudiant_id, source, marquee_at) VALUES
    (1100, 1000, 10, 'ETUDIANT',  (NOW() AT TIME ZONE 'UTC')),
    (1101, 1000, 11, 'ETUDIANT',  (NOW() AT TIME ZONE 'UTC')),
    (1102, 1000, 12, 'FORMATEUR', (NOW() AT TIME ZONE 'UTC'));

INSERT INTO exercice (id, session_id, etudiant_id, lien, statut, depose_at) VALUES
    (2000, 1000, 10, 'https://github.com/exemple/awa-tp4',     'RELU',       (NOW() AT TIME ZONE 'UTC')),
    (2001, 1000, 11, 'https://github.com/exemple/bilal-tp4',   'EN_ATTENTE', (NOW() AT TIME ZONE 'UTC')),
    (2002, 1000, 12, 'https://github.com/exemple/chantal-tp4', 'EN_ATTENTE', (NOW() AT TIME ZONE 'UTC'));

INSERT INTO relecture (id, exercice_id, relecteur_id, statut, note, commentaire, assignee_at, rendue_at) VALUES
    -- Rendue : Awa a donc une moyenne. Bilal n'a plus rien à relire.
    (3000, 2000, 11, 'RENDUE', 15,
     'Structure claire, il manque les tests du service.',
     (NOW() AT TIME ZONE 'UTC'), (NOW() AT TIME ZONE 'UTC')),

    -- Assignées et non rendues : elles comptent dans relecturesEnAttente (RG11)
    -- et laissent Bilal et Chantal sans moyenne — « null » et non « zéro ».
    (3001, 2001, 12, 'ASSIGNEE', NULL, NULL, (NOW() AT TIME ZONE 'UTC'), NULL),
    (3002, 2002, 10, 'ASSIGNEE', NULL, NULL, (NOW() AT TIME ZONE 'UTC'), NULL);

-- Les séquences repartent après les identifiants posés à la main.
ALTER TABLE session   ALTER COLUMN id RESTART WITH 2000;
ALTER TABLE presence  ALTER COLUMN id RESTART WITH 2000;
ALTER TABLE exercice  ALTER COLUMN id RESTART WITH 3000;
ALTER TABLE relecture ALTER COLUMN id RESTART WITH 4000;
