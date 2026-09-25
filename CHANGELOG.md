# Journal des versions

Toutes les entrées renvoient à l'issue ou à la pull request qui les porte, et
correspondent à des commits réellement présents dans l'historique. Les
versions sont marquées par des tags annotés : `v0.1`, `v0.2`, `v1.0`.

Les trois commits notés au barème sont, dans l'ordre : `[JALON] analyse`,
`[JALON] v0.1`, `[JALON] v1.0`.

---

## v1.0 — 25/09/2026 · étape 4, version finale

Tag `v1.0`, commit `[JALON] v1.0`.

L'étape 4 n'ajoute **aucune fonctionnalité** : elle clôt le périmètre, trie ce
qui reste et rend le dépôt lisible par quelqu'un qui n'a que le `README`.

### Ajouté
- **`CHANGELOG.md`** — ce fichier.

### Modifié
- `api/contrat.yaml` et sa copie racine passent en **`version: "2.0"`**, ce que
  leur description annonçait déjà depuis la révision 2 de l'étape 3.
- `README.md` corrigé après avoir été **rejoué à la lettre depuis un clone
  vierge**, dans un dossier vide (voir « Vérifications » plus bas).
- `docs/CAHIER_DES_CHARGES.md`, section 3 — le périmètre **exclu** dit
  désormais explicitement ce qui n'est pas livré d'EF13 et d'EF9, et pourquoi.

### Décidé, et écrit plutôt que taire
- **Le frontend d'EF13 n'est pas construit.** Sans le tirage double,
  `noteProvisoire` vaudrait toujours `false` et `commentaires` n'aurait jamais
  plus d'un élément : l'écran aurait montré la forme du nouveau modèle sans sa
  substance. Un périmètre réduit et annoncé vaut mieux qu'un écran qui ment.
- **Les cinq issues restantes** (#8, #9, #10, #11, #23) portent chacune un
  commentaire final disant pourquoi elle reste ouverte et ce qu'il faudrait
  exactement pour la fermer. Aucune n'est laissée sans explication.

---

## v0.2 — 25/09/2026 · étape 3, enveloppe

Tag `v0.2`, commit `a826c8e`.

Deux sujets sans rapport, traités sur **deux branches et deux pull requests
séparées**, comme l'enveloppe l'exige.

### Corrigé — le bug signalé par le client · issue #22 · PR #24

> « Ils ont tapé le code presque en même temps et il n'y en a qu'un seul qui
> apparaît dans ma liste. »

L'ordre des commits est la démonstration : le test **rouge** (`9db220f`) est
poussé **avant** toute correction. Trois défauts distincts, trois commits.

1. **Le couplage** (`bf777a8`) — *le défaut central*. Le rattrapage RG22 vivait
   dans la transaction de la présence, donc son échec annulait une écriture
   sans rapport avec lui. Le client disait « ma présence disparaît », pas « le
   tirage échoue ». Le rejeu part désormais dans sa propre transaction, après
   le commit de la présence.
2. **La course** (`0c57849`) — deux rattrapages concurrents lisaient le même
   exercice `DEPOSE` et violaient `UNIQUE (exercice_id)`. Verrou pessimiste sur
   la ligne, plutôt qu'une violation rattrapée.
3. **Le `500`** (`c8a3889`) — une violation d'intégrité concurrente sortait en
   `500 ERREUR_INTERNE`, ce que ENF4 interdit. Nouveau code au catalogue :
   **`409 CONFLIT_CONCURRENT`**, documenté et reproductible (`a46c10c`).

### Analysé — le changement de besoin · issue #23 · PR #25

Le client révoque **Q6** : deux relecteurs par exercice, note moyenne, note
provisoire tant qu'une seule relecture est rendue.

- **Analyse** (`91c8e51`) — section 7 gagne une **troisième catégorie de
  difficulté** : Q6 n'est ni ambiguë ni incomplète, elle est **périmée**.
  Première fois qu'une réponse du client en contredit une autre *dans le temps*.
  Elle est donc barrée et remplacée, jamais réécrite. Hypothèses **H13 à H19**,
  **EF13** créée, **RG6** remplacée, **RG7/RG19/RG22** réécrites,
  **RG23/RG24/RG25** ajoutées. Contrat en révision 2, **D2** et **D4** corrigés.
- **Migration `V2`** (`86fd4e0`) — **ajoutée**, jamais une modification de `V1`.
  Vérifiée sur une base PostgreSQL **déjà remplie** : ancienne contrainte
  retirée, `uk_relecture_exercice_relecteur` en place, données intactes.
  Aucune seconde relecture n'est inventée pour les exercices déjà relus (H14).

### Re-priorisé
- **EF10 (#10) sort du périmètre** pour absorber un *Must* arrivé tard. C'est la
  seule exigence du lot qui réponde à une crainte (Q4) et non à un usage.
  Reportée, pas abandonnée.
- **EF9 (#9) est explicitement protégée** : c'est l'écran où la note provisoire
  doit apparaître.

### Hygiène
- `937c934` — l'énoncé n'était exclu que dans `Docs_fournis/` ; un `git add -A`
  distrait l'aurait publié. Et `docs/ERREURS.md` ne disait pas que le code
  `DEMO01` n'est valable que quinze minutes : trois de ses commandes, dont le
  cas nominal, auraient renvoyé `410` chez le correcteur.
- `a826c8e` — analyse alignée sur la **mise à jour du sujet du 25/09** :
  épreuve Git supprimée, démarche renumérotée en cinq étapes, vocabulaire
  unifié sur « issue ».

---

## v0.1 — 25/09/2026 · étape 2, première version

Tag `v0.1`, commit `[JALON] v0.1`.

Les sept exigences **Must**, plus une **Could** arrivée en bonus. Une branche
par issue, une pull request par branche, le commit de fusion fermant l'issue.

| Exigence | Issue | PR | Ce qu'elle apporte |
|---|---|---|---|
| **EF1** — ouvrir une séance | #1 | #13 | Socle Spring Boot, schéma Flyway `V1`, erreurs centralisées, horloge injectable, socle React |
| **EF2** — marquer sa présence | #2 | #21 | Les quatre issues de D3 : `201`, `400 CODE_INCONNU`, `409 DEJA_PRESENT`, `410 CODE_EXPIRE` |
| **EF3** — déposer son exercice | #3 | #15 | RG12 : le dépôt survit à l'expiration du code, jusqu'à la clôture |
| **EF4** — tirer un relecteur | #4 | #16 | RG22 : l'exercice orphelin est rattrapé à chaque nouvelle présence |
| **EF5** — rendre une note | #5 | #17 | H12 : le contrat ne disait pas *qui* relit, rendant `403 AUTO_RELECTURE` inatteignable |
| **EF6** — clôturer une séance | #6 | #18 | **L'opération absente du contrat imposé** (H1) |
| **EF7** — tableau de suivi | #7 | #19 | La moyenne calculée par l'API et nulle part ailleurs (F3) |
| **EF12** — lister les séances | #12 | #18 | *Could*, arrivée avec EF6 : le formateur doit retrouver la séance à clôturer |
| Démarrage | — | #20 | `docker compose up`, jeu de démonstration, **`docs/ERREURS.md`** |

### Le trou du sujet, comblé en EF6
Aucune des cinq opérations imposées ne **clôturait** une séance, alors que Q10 et
Q12 y conditionnent deux règles majeures. Trois notions sont désormais
distinctes de la base jusqu'aux libellés à l'écran : **expiration du code**
(RG2), **séance expirée** (les dépôts restent possibles, RG12), **clôture**
(tout est figé, irréversible, RG20).

### Deux erreurs silencieuses corrigées en chemin
- **Surefire n'exécutait pas les `*IT.java`** : `./mvnw test` affichait un vert
  parfait en ignorant le seul test qui vérifiait la conformité au contrat.
- **`tsconfig.tsbuildinfo` était suivi par Git** : un fichier généré, qui
  resalissait l'arbre à chaque build.

---

## Avant — étape 1, analyse

Commit `[JALON] analyse`, poussé **avant la première ligne de code**.

Cahier des charges (10 sections imposées, 12 exigences fonctionnelles, 22 règles
de gestion, 11 hypothèses tranchées), quatre diagrammes Mermaid, `contrat.yaml`
figé — les 5 opérations imposées reprises à la lettre, vérifiées par
comparaison automatique — et 12 issues avec critères d'acceptation, priorité
MoSCoW et référence `EFx`/`RGx`.

**Les deux pièges du sujet y sont tranchés par écrit** : la contradiction
**Q10 contre Q15** (Q10 retenu — une règle bornée et testable l'emporte sur une
intention sans borne) et le trou **H1**, la clôture de séance que rien ne
permettait.
