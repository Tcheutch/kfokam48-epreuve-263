# D2 — Modèle de données

Ce diagramme fait foi pour les migrations Flyway (`backend/src/main/resources/db/migration`).
Toute évolution du schéma se fait par **une nouvelle migration** et une mise à jour
de ce diagramme dans le même commit (contrainte B5).

> **Étape 3 — ce diagramme a changé.** Le client a révoqué Q6 : un exercice est
> désormais relu par **deux** pairs distincts. Les éléments barrés ci-dessous
> sont ce qui était vrai au jalon `v0.1` ; ils sont conservés pour que
> l'historique reste lisible.

```mermaid
erDiagram
    PROMOTION {
        Long id PK
        String nom "unique"
    }
    UTILISATEUR {
        Long id PK
        String nom
        String role "FORMATEUR | ETUDIANT (RG1)"
        Long promotion_id FK "nullable pour un formateur"
    }
    SESSION {
        Long id PK
        String titre "non vide"
        String code UK "unique parmi les sessions actives (RG21)"
        DateTime ouverture_at
        DateTime expiration_at "= ouverture_at + 15 min (RG2)"
        DateTime cloture_at "nullable ; non nulle = session clôturée (RG20)"
        Long promotion_id FK
        Long formateur_id FK
    }
    PRESENCE {
        Long id PK
        String source "ETUDIANT | FORMATEUR (RG14)"
        DateTime marquee_at
        Long session_id FK
        Long etudiant_id FK
    }
    EXERCICE {
        Long id PK
        String lien "URL http(s) absolue (RG18)"
        String statut "DEPOSE | EN_ATTENTE | PARTIELLEMENT_RELU | RELU (voir D4)"
        DateTime depose_at
        Long session_id FK
        Long etudiant_id FK
    }
    RELECTURE {
        Long id PK
        String statut "ASSIGNEE | RENDUE (RG11)"
        Integer note "nullable tant que ASSIGNEE ; entier 0..20 (RG9)"
        String commentaire "nullable tant que ASSIGNEE"
        DateTime assignee_at
        DateTime rendue_at "nullable"
        Long exercice_id FK
        Long relecteur_id FK
    }
    TENTATIVE_CODE {
        Long id PK
        Long etudiant_id FK
        Integer echecs_consecutifs "remis à 0 après un succès (RG4)"
        DateTime bloque_jusqua "nullable"
    }

    PROMOTION  ||--o{ UTILISATEUR : "regroupe"
    PROMOTION  ||--o{ SESSION     : "concerne"
    UTILISATEUR ||--o{ SESSION    : "anime (formateur)"
    SESSION    ||--o{ PRESENCE    : "enregistre"
    UTILISATEUR ||--o{ PRESENCE   : "est marqué présent"
    SESSION    ||--o{ EXERCICE    : "reçoit"
    UTILISATEUR ||--o{ EXERCICE   : "dépose"
    EXERCICE   ||--o{ RELECTURE   : "est relu par deux pairs distincts (RG6 étape 3, RG24)"
    UTILISATEUR ||--o{ RELECTURE  : "relit"
    UTILISATEUR ||--o| TENTATIVE_CODE : "cumule ses échecs"
```

## Contraintes portées par les migrations

| Table | Contrainte | Règle | Code d'erreur |
|---|---|---|---|
| `presence` | `UNIQUE (session_id, etudiant_id)` | RG15 | `409 DEJA_PRESENT` |
| `exercice` | `UNIQUE (session_id, etudiant_id)` | RG16 | `409 EXERCICE_DEJA_DEPOSE` |
| `relecture` | ~~`UNIQUE (exercice_id)`~~ **retirée en V2** — elle matérialisait « un seul relecteur » | ~~RG6 (Q6)~~ périmée | — |
| `relecture` | `UNIQUE (exercice_id, relecteur_id)` **ajoutée en V2** — deux relecteurs *distincts* | RG24 | `409 CONFLIT_CONCURRENT` |
| `relecture` | `CHECK (note IS NULL OR note BETWEEN 0 AND 20)` | RG9 | `400 NOTE_INVALIDE` |
| `relecture` | `CHECK (relecteur_id <> exercice.etudiant_id)` — vérifié en service | RG5 | `403 AUTO_RELECTURE` |
| `session` | `UNIQUE (code)` **global** — et non restreint aux sessions actives comme envisagé ici au départ : un index partiel n'est pas portable sur H2, où tournent les tests. Une unicité globale est plus forte, donc toujours suffisante pour RG21 | RG21 | — |
| `presence` / `exercice` | `source` et `statut` stockés en `VARCHAR` + `CHECK`, pas en `ENUM` natif | portabilité H2 / PostgreSQL | — |

## Choix de modélisation

- **Une seule table `utilisateur`** avec un champ `role`, plutôt que deux tables :
  un formateur et un étudiant partagent exactement les mêmes attributs, et
  `presence`, `exercice`, `relecture` pointent toutes vers la même clé.
- **La `relecture` est créée dès l'assignation**, au statut `ASSIGNEE`, note nulle.
  C'est ce que suppose le contrat imposé : `POST /api/relectures/{id}` reçoit un
  identifiant de relecture **existant**. C'est aussi ce qui rend calculable
  `relecturesEnAttente` du tableau (RG11, Q11).
- **`TENTATIVE_CODE` est persistée** et non gardée en mémoire : le blocage de
  deux minutes (RG4, Q4) doit survivre à un redémarrage, sinon la règle se
  contourne trivialement.
- **`moyenne` n'est stockée nulle part** : elle est recalculée par l'API à la
  lecture du tableau (RG19, contrainte F3). Depuis l'étape 3, c'est une
  **moyenne de moyennes** — note d'exercice d'abord, moyenne de l'étudiant
  ensuite (H13). Ne rien stocker évite d'avoir à réécrire des notes existantes
  quand la règle de calcul change, ce qui vient précisément d'arriver.
- **La cardinalité `exercice → relecture` passe de 0..1 à 0..2** (étape 3).
  Le changement se fait par une migration **V2 ajoutée**, jamais en modifiant
  `V1__schema_initial.sql` : retrait de `UNIQUE (exercice_id)`, ajout de
  `UNIQUE (exercice_id, relecteur_id)`. Les lignes existantes — un relecteur
  par exercice — restent valides sous la nouvelle contrainte, et aucune
  seconde relecture ne leur est inventée (H14).
