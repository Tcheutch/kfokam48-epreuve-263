-- ---------------------------------------------------------------------------
-- V2 — Deux relecteurs par exercice (EF13, étape 3).
--
-- Migration AJOUTÉE. V1__schema_initial.sql n'est pas touchée : elle est
-- appliquée chez le client, son empreinte est enregistrée, la modifier ferait
-- échouer la validation Flyway au prochain démarrage (contrainte B5).
--
-- Ce que le client a révoqué : Q6, « un seul relecteur ». Chaque exercice est
-- désormais relu par deux pairs DISTINCTS (RG6 révisée, RG24).
--
-- SURVIE DES DONNÉES EXISTANTES (hypothèse H14) : les exercices déjà relus
-- n'ont qu'une relecture. Cette migration ne leur en invente pas de seconde —
-- une note qu'aucun humain n'a donnée n'est pas une note. Ils restent à une
-- relecture, donc « provisoires » au sens de RG23, sauf si leur séance est
-- clôturée, auquel cas plus rien ne peut changer et la note est définitive.
-- Toutes les lignes existantes satisfont déjà la nouvelle contrainte.
-- ---------------------------------------------------------------------------

-- 1. Retirer l'unicité qui matérialisait « un seul relecteur par exercice ».
--
--    Elle avait été déclarée en ligne dans V1 (`exercice_id BIGINT NOT NULL
--    UNIQUE`), donc sans nom explicite : chaque moteur lui en a donné un.
--    PostgreSQL la nomme `relecture_exercice_id_key` — vérifié sur le
--    conteneur. H2, utilisé par les tests, génère un nom de la forme
--    `CONSTRAINT_xxx`, imprévisible d'une base à l'autre.
--
--    Leçon pour la suite : toute contrainte doit être NOMMÉE à sa création,
--    sinon elle devient très difficile à retirer de façon portable. Les
--    contraintes nommées de V1 (`uk_presence_session_etudiant`, `ck_*`, `fk_*`)
--    ne poseront pas ce problème.
ALTER TABLE relecture DROP CONSTRAINT IF EXISTS relecture_exercice_id_key;

-- 2. Deux relecteurs, mais DISTINCTS (RG24).
--    Sans cette contrainte, rien n'empêcherait le tirage de désigner deux fois
--    la même personne, ce qui viderait « deux pairs différents » de son sens.
ALTER TABLE relecture
    ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- 3. L'exercice gagne un état : PARTIELLEMENT_RELU (RG23, H18).
--    Une seule des deux relectures rendue -> la note est PROVISOIRE.
--    Le CHECK de V1 ayant été déclaré nommément, il se remplace proprement.
ALTER TABLE exercice DROP CONSTRAINT IF EXISTS ck_exercice_statut;
ALTER TABLE exercice
    ADD CONSTRAINT ck_exercice_statut
        CHECK (statut IN ('DEPOSE', 'EN_ATTENTE', 'PARTIELLEMENT_RELU', 'RELU'));

-- Aucune donnée n'est réécrite : les statuts déjà posés restent valides, seul
-- leur périmètre se précise. `DEPOSE` signifie désormais « aucun relecteur
-- assigné » plutôt que « aucun relecteur disponible ».
