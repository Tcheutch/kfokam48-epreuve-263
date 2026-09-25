# KFOKAM48 — Présence & Relecture

Application de suivi de présence et de relecture croisée entre étudiants,
pour un centre de formation. Épreuve finale fullstack KFOKAM48 — matricule **263**.

> **État actuel : étape 1 terminée (analyse).** Aucun code applicatif n'est encore
> écrit, volontairement : le cahier des charges, les diagrammes, le backlog et le
> contrat d'API sont figés avant la première ligne. Ce fichier sera complété à
> l'étape 2 avec les commandes de démarrage réelles, testées depuis un clone vierge.

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
3. Les **issues** du dépôt : une par exigence fonctionnelle, avec critères
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

*À compléter à l'étape 2, puis testé depuis un clone vierge dans un dossier vide
avant la soumission (ENF6).*

```bash
# prévu
docker compose up
# → API   http://localhost:8080
# → Front http://localhost:5173
```

## Conventions

- **Tout est en français** : documentation, commits, issues, messages d'erreur,
  noms du domaine (`Session`, `Presence`, `Exercice`, `Relecture`, `Promotion`).
- **Une branche par issue, une PR par branche**, le commit de fusion ferme l'issue.
- Les tests nomment la règle qu'ils couvrent : `rg2_codeExpireApres15Minutes`.
