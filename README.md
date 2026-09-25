# KFOKAM48 — Présence & Relecture

Application de suivi de présence et de relecture croisée entre étudiants,
pour un centre de formation. Épreuve finale fullstack KFOKAM48 — matricule **263**.

> **État actuel : étape 2 — première version.** Les sept exigences **Must**
> (EF1 à EF7) sont livrées, plus EF12 en bonus. Les exigences **Should**
> (EF8 à EF11) arrivent à l'étape 4.

> **Note sur le premier commit.** `[JALON] depart vO.1` est le commit vide de
> vérification de poussée demandé par le LISEZ-MOI de l'épreuve (§2.4), fait avant
> toute analyse. Ce n'est **pas** le jalon `v0.1`, qui viendra à sa place après
> l'étape 2. Les trois jalons notés sont, dans l'ordre : `[JALON] analyse`,
> `[JALON] v0.1`, `[JALON] v1.0`.

---

## Le besoin en trois phrases

Un formateur ouvre une session de cours et obtient un code de présence valable
15 minutes. Les étudiants marquent leur présence avec ce code, puis déposent le
lien de leur exercice ; le système tire au sort, parmi les présents, un relecteur
qui n'est jamais l'auteur. Le formateur suit dans un tableau, par étudiant, les
présences, les dépôts, la moyenne des notes reçues et les relectures en retard.

---

## Structure du dépôt

```
docs/
  CAHIER_DES_CHARGES.md     Les 10 sections imposées : EF, RG, hypothèses tranchées
  ERREURS.md                Une commande curl par code d'erreur du contrat
  JOURNAL.md                Une entrée par étape, écrite à la fin de l'étape
  diagrammes/
    D1_cas_utilisation.md   Cas d'utilisation (Mermaid)
    D2_modele_donnees.md    Modèle de données — fait foi pour les migrations
    D3_sequence_presence.md Séquence « marquer sa présence », erreurs comprises
    D4_etats_transitions.md Cycle de vie d'un exercice (bonus)
api/
  contrat.yaml              Contrat d'API figé — copie identique à la racine
backend/                    Spring Boot, Java 17, Maven (étape 2)
frontend/                   React + Vite (étape 2)
```

## Par où commencer la lecture

1. **`docs/CAHIER_DES_CHARGES.md`**, et en priorité sa **section 7** : c'est là que
   sont tranchées la contradiction du client (Q10 contre Q15) et les onze zones
   d'ombre, dont le trou que personne n'avait comblé — rien, dans le contrat
   imposé, ne permettait de **clôturer une session**, alors que deux règles
   majeures en dépendent.
2. **`api/contrat.yaml`** : les 5 opérations imposées reprises à la lettre, et 7
   opérations ajoutées dont chacune renvoie à l'hypothèse qui la justifie.
3. **[`docs/ERREURS.md`](docs/ERREURS.md)** — une commande `curl` par code
   d'erreur du contrat, avec les identifiants du jeu de démonstration. À lire
   **avant de tester l'API à la main** : deux codes ne se déclenchent pas avec
   un appel naïf, et c'est une décision d'analyse, pas un oubli.
   `403 AUTO_RELECTURE` exige le champ facultatif `relecteurId` (H12), et
   `409 RELECTURE_DEJA_RENDUE` ne tombe qu'après la clôture de la séance
   (arbitrage Q10 contre Q15).
4. Les **issues** du dépôt : une par exigence fonctionnelle, avec critères
   d'acceptation, priorité MoSCoW et référence `EFx` / `RGx`.

---

## Choix techniques

| | Choix | Pourquoi, en une ligne |
|---|---|---|
| **Backend** | Spring Boot 3, Java 17, Maven (`./mvnw`) | Imposé (B1) |
| **Frontend** | **React 18 + Vite + TypeScript** | Choisi pour son démarrage sans configuration et parce que le périmètre — trois écrans de formulaires — ne justifie ni le routage intégré ni le rendu serveur de Next.js (F1) |
| **Base de données** | PostgreSQL en exécution, H2 en mémoire pour les tests | Les tests doivent passer sur un poste sans base locale (B6, ENF7) |
| **Migrations** | Flyway, versionnées dès la première | `ddl-auto=update` est interdit (B5), et l'étape 3 fera bouger le schéma |
| **Erreurs** | `@RestControllerAdvice` unique | Toute erreur sort en `{ code, message }` français, sans exception (B4, ENF4) |

Le soin visuel n'est pas évalué : aucun temps n'est investi en CSS.

---

## Démarrage

Une seule commande, depuis un clone vierge. Rien d'autre à installer que Docker.

```bash
docker compose up --build
```

- Application : **http://localhost:5173**
- API : **http://localhost:8080/api/promotions**

Les données de démonstration sont chargées au démarrage par Flyway :
deux promotions, huit étudiants, et **trois séances dans trois états différents** —
une ouverte, une dont le code a expiré, une clôturée — avec des présences, des
exercices et des relectures. L'application n'est jamais vide au premier écran, et
chaque code d'erreur du contrat est reproductible immédiatement : voir
[`docs/ERREURS.md`](docs/ERREURS.md).

Pour repartir d'une base entièrement vierge :

```bash
docker compose down && docker compose up --build
```

Aucun volume n'est déclaré, donc `down` suffit à tout effacer et les migrations
sont rejouées depuis zéro (ENF5).

### Développement, sans Docker

```bash
docker compose up -d db                       # PostgreSQL seul, publié sur 5433
cd backend  && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/kfokam48 \
               ./mvnw spring-boot:run         # API sur :8080
cd frontend && npm install && npm run dev     # front sur :5173, proxy vers :8080
```

> Le conteneur est publié sur **5433** et non 5432 : beaucoup de postes ont déjà
> un PostgreSQL sur le port standard, et la collision produit un
> `password authentication failed` trompeur — on croit parler au conteneur, on
> parle à la base locale. Dans la pile `docker compose`, le backend passe par le
> réseau interne et ce port n'intervient pas.

Toujours passer par `./mvnw`, jamais par un `mvn` du système : la version de
Maven est celle que le dépôt déclare.

### Tests

```bash
cd backend && ./mvnw test      # 76 tests, aucune base de données requise
cd frontend && npm run build   # TypeScript strict
```

Les tests d'intégration tournent sur H2 en mémoire **avec les migrations Flyway
réellement livrées** : ils vérifient le schéma de production, pas une copie.

## Conventions

- **Tout est en français** : documentation, commits, issues, messages d'erreur,
  noms du domaine (`Session`, `Presence`, `Exercice`, `Relecture`, `Promotion`).
- **Une branche par issue, une PR par branche**, le commit de fusion ferme l'issue.
- Les tests nomment la règle qu'ils couvrent : `rg2_codeExpireApres15Minutes`.
