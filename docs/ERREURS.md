# Reproduire chaque code d'erreur du contrat

> **À quoi sert ce document.** Le contrat impose que *toute* erreur sorte au
> format `{ "code": "...", "message": "..." }`. Deux de ces codes ne se
> déclenchent pas avec un appel naïf, pour des raisons qui sont des décisions
> d'analyse, pas des oublis :
>
> - **`403 AUTO_RELECTURE`** — le corps imposé ne dit pas *qui* relit. Sans le
>   champ facultatif `relecteurId` (hypothèse **H12**), l'erreur est
>   inatteignable : le tirage (RG7) interdit déjà d'assigner l'auteur.
> - **`409 RELECTURE_DEJA_RENDUE`** — l'arbitrage **Q10 contre Q15** (section 7
>   du cahier des charges) autorise le relecteur à corriger sa note tant que la
>   séance est ouverte. Rendre deux fois de suite renvoie donc `200`, puis `200`.
>   Le `409` tombe **après la clôture**.
>
> Chaque ligne ci-dessous est une commande à copier-coller, avec les
> identifiants fixes du jeu de démonstration
> (`backend/src/main/resources/db/demo/`).

Démarrer avant tout :

```bash
docker compose up --build
```

## Le jeu de démonstration

| Séance | `id` | Code | État | Ce qu'elle permet de montrer |
|---|---|---|---|---|
| Spring Boot — jour 4 | `1000` | `DEMO01` | ouverte | le cas nominal |
| React — jour 3 | `1001` | `DEMO02` | **code expiré** (ouverte il y a 16 min) | `410` sans attendre |
| SQL — jour 2 | `1002` | `DEMO03` | **clôturée** | `409 SESSION_CLOTUREE` |

| Étudiant | `id` | Promotion | Situation |
|---|---|---|---|
| Awa Ndiaye | `10` | 1 | présente, exercice `2000` **relu** (note 15), doit relire `2002` |
| Bilal Moussa | `11` | 1 | présent, exercice `2001` en attente, ne doit plus rien |
| Chantal Fotso | `12` | 1 | présence **ajoutée par le formateur**, exercice `2002`, doit relire `2001` |
| Diane, Emeka, Fatou | `13` `14` `15` | 1 | n'ont rien fait — ils apparaissent quand même au tableau |
| Grace Ateba | `20` | **2** | sert à déclencher `ETUDIANT_HORS_PROMOTION` |

Relectures : `3000` rendue (note 15), `3001` assignée à Chantal sur l'exercice
de Bilal, `3002` assignée à Awa sur l'exercice de Chantal.

---

## Les cas nominaux

```bash
# 201 — ouvrir une séance (EF1)
curl -i -X POST localhost:8080/api/sessions \
  -H 'Content-Type: application/json' \
  -d '{"titre":"Nouvelle séance","promotionId":1}'

# 201 — marquer sa présence (EF2)
curl -i -X POST localhost:8080/api/presences \
  -H 'Content-Type: application/json' \
  -d '{"code":"DEMO01","etudiantId":13}'

# 201 — déposer son exercice (EF3)
curl -i -X POST localhost:8080/api/exercices \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":1000,"etudiantId":13,"lien":"https://github.com/exemple/tp"}'

# 200 — rendre une relecture (EF5)
curl -i -X POST localhost:8080/api/relectures/3001 \
  -H 'Content-Type: application/json' \
  -d '{"note":14,"commentaire":"Bon travail."}'

# 200 — le tableau (EF7)
curl -s 'localhost:8080/api/tableau?promotionId=1'
```

## Les erreurs

Chaque commande renvoie le statut indiqué **et** un corps `{ code, message }`.

| Code | HTTP | Commande |
|---|---|---|
| `CHAMP_MANQUANT` | 400 | ```curl -i -X POST localhost:8080/api/sessions -H 'Content-Type: application/json' -d '{"promotionId":1}'``` |
| `PROMOTION_INCONNUE` | **400 ici** | ```curl -i -X POST localhost:8080/api/sessions -H 'Content-Type: application/json' -d '{"titre":"X","promotionId":999}'``` — le contrat n'autorise que 400 sur cette opération |
| `CODE_INCONNU` | 400 | ```curl -i -X POST localhost:8080/api/presences -H 'Content-Type: application/json' -d '{"code":"ZZZZZZ","etudiantId":14}'``` |
| `CODE_EXPIRE` | **410** | ```curl -i -X POST localhost:8080/api/presences -H 'Content-Type: application/json' -d '{"code":"DEMO02","etudiantId":14}'``` — séance ouverte il y a 16 min |
| `DEJA_PRESENT` | 409 | ```curl -i -X POST localhost:8080/api/presences -H 'Content-Type: application/json' -d '{"code":"DEMO01","etudiantId":10}'``` — Awa est déjà présente |
| `ETUDIANT_HORS_PROMOTION` | 400 | ```curl -i -X POST localhost:8080/api/presences -H 'Content-Type: application/json' -d '{"code":"DEMO01","etudiantId":20}'``` — Grace est en promotion 2 |
| `SESSION_CLOTUREE` | 409 | ```curl -i -X POST localhost:8080/api/presences -H 'Content-Type: application/json' -d '{"code":"DEMO03","etudiantId":14}'``` |
| `LIEN_INVALIDE` | 400 | ```curl -i -X POST localhost:8080/api/exercices -H 'Content-Type: application/json' -d '{"sessionId":1000,"etudiantId":14,"lien":"exemple.com/tp"}'``` — URL non absolue |
| `EXERCICE_DEJA_DEPOSE` | 409 | ```curl -i -X POST localhost:8080/api/exercices -H 'Content-Type: application/json' -d '{"sessionId":1000,"etudiantId":10,"lien":"https://exemple.com/x"}'``` |
| `NOTE_INVALIDE` | 400 | ```curl -i -X POST localhost:8080/api/relectures/3001 -H 'Content-Type: application/json' -d '{"note":15.5,"commentaire":"X"}'``` — décimale, refusée et non arrondie (RG9) |
| `AUTO_RELECTURE` | **403** | ```curl -i -X POST localhost:8080/api/relectures/3001 -H 'Content-Type: application/json' -d '{"note":20,"commentaire":"X","relecteurId":11}'``` — `11` est **l'auteur** de l'exercice `2001`. **Le champ `relecteurId` est indispensable ici** (H12) |
| `RELECTEUR_NON_ASSIGNE` | 403 | ```curl -i -X POST localhost:8080/api/relectures/3001 -H 'Content-Type: application/json' -d '{"note":14,"commentaire":"X","relecteurId":13}'``` — Diane n'est pas la relectrice assignée |
| `RELECTURE_INCONNUE` | 404 | ```curl -i -X POST localhost:8080/api/relectures/999999 -H 'Content-Type: application/json' -d '{"note":14,"commentaire":"X"}'``` |
| `EXERCICE_INCONNU` | 404 | ```curl -i -X PUT localhost:8080/api/exercices/999999 -H 'Content-Type: application/json' -d '{"lien":"https://exemple.com/x"}'``` |
| `SESSION_INCONNUE` | 404 | ```curl -i -X POST localhost:8080/api/sessions/999999/cloture``` |
| `PROMOTION_INCONNUE` | **404** | ```curl -i 'localhost:8080/api/tableau?promotionId=999'``` — 404 ici, le contrat l'y autorise |
| `RESSOURCE_INCONNUE` | 404 | ```curl -i localhost:8080/api/nexiste-pas``` — même un 404 imprévu sort au format imposé (ENF4) |

### Les deux cas qui demandent une mise en scène

**`409 RELECTURE_DEJA_RENDUE`** — l'arbitrage Q10 fait que rendre deux fois
d'affilée renvoie `200`, puis `200` : c'est **voulu**, le relecteur corrige sa
note tant que la séance est ouverte (RG10). Le `409` tombe après la clôture.

```bash
# 1. rendre la relecture — 200
curl -i -X POST localhost:8080/api/relectures/3001 \
  -H 'Content-Type: application/json' -d '{"note":14,"commentaire":"Bien."}'

# 2. la corriger, séance encore ouverte — 200, et la moyenne est recalculée
curl -i -X POST localhost:8080/api/relectures/3001 \
  -H 'Content-Type: application/json' -d '{"note":16,"commentaire":"Relu."}'

# 3. le formateur clôture la séance — 200, état CLOTUREE
curl -i -X POST localhost:8080/api/sessions/1000/cloture

# 4. corriger de nouveau — 409 RELECTURE_DEJA_RENDUE
curl -i -X POST localhost:8080/api/relectures/3001 \
  -H 'Content-Type: application/json' -d '{"note":18,"commentaire":"Trop tard."}'
```

> La clôture de l'étape 3 fige la séance `1000` : les commandes du tableau
> ci-dessus qui la visent renverront ensuite `409 SESSION_CLOTUREE`.
> Pour repartir à neuf : `docker compose down && docker compose up --build`.

**`400 TROP_DE_TENTATIVES`** — RG4 (cinq codes erronés, deux minutes de
blocage) relève d'**EF10, priorité Should**. Elle n'est pas livrée au jalon
`v0.1` ; elle arrive à l'étape 4. Le code d'erreur est déjà au contrat et au
catalogue, l'endpoint ne le renvoie pas encore. Annoncé plutôt que promis.

**`500 ERREUR_INTERNE`** — c'est le filet de sécurité du
`@RestControllerAdvice` : il ne se déclenche que sur une erreur imprévue, donc
par construction il n'a pas de commande de reproduction. Sa présence est
vérifiée par le fait qu'aucune trace d'exécution ne sort jamais, y compris sur
un corps JSON illisible :

```bash
curl -i -X POST localhost:8080/api/sessions \
  -H 'Content-Type: application/json' -d '{ pas du json'
# 400 { "code": "CHAMP_MANQUANT", ... } — et pas une page d'erreur Spring
```
