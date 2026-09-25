# Journal de bord — 263

> Une entrée **par étape**, écrite **au moment où je la termine**.
> Fait / Bloqué / IA — et pour l'IA, comment j'ai vérifié sa réponse.

---

## Étape 1 — Analyse et conception

**Fait :** cahier des charges complet (les 10 sections imposées, 12 exigences
fonctionnelles, 7 non fonctionnelles, 22 règles de gestion, 11 hypothèses
tranchées) ; les quatre diagrammes en Mermaid — D1 cas d'utilisation, D2 modèle
de données, D3 séquence de « marquer sa présence » avec ses trois cas d'erreur,
D4 états-transitions de l'exercice (bonus) ; `api/contrat.yaml` complété et figé,
avec copie identique à la racine ; `.gitignore` Java + JS posé **avant** toute
ligne de code ; 12 issues créées, une par exigence, avec critères d'acceptation,
priorité MoSCoW, référence `EFx`/`RGx` et jalon `v0.1` ou `v1.0` ; README avec le
choix du frontend justifié. Puis `[JALON] analyse`.

**Bloqué :**

- **~15 min sur la contradiction Q10 / Q15.** Q10 autorise le relecteur à
  corriger sa note jusqu'à la clôture, Q15 la déclare définitive dès l'envoi.
  Tranché en faveur de **Q10** : Q10 nomme un événement précis et vérifiable (la
  clôture), Q15 énonce une intention morale (« c'est plus honnête ») sans borne
  technique. Entre une règle testable et une intention, j'implémente la règle.
  Argument qui a emporté la décision : en retenant Q10, il suffit au formateur de
  clôturer pour obtenir le comportement de Q15 — l'inverse serait impossible.
  Écrit en section 7, matérialisé par l'issue #11.

- **~20 min sur le trou du sujet.** En écrivant D4, je butais sur « qui fait
  passer l'exercice en définitif ». Rien, dans les 5 opérations imposées, ne
  **clôture** une session — alors que Q10 et Q12 y conditionnent deux règles
  majeures et que le client parle de « clôturer » comme d'une évidence. J'ai
  d'abord cru à une erreur de ma part, puis relu les cinq opérations une par une :
  le trou est réel. Décision : ajouter `POST /api/sessions/{id}/cloture`, un champ
  `cloture_at` nullable, EF6 et RG20, et surtout **séparer trois notions que le
  client confond** — expiration du code (15 min, bloque les présences), fin de
  session, et clôture (irréversible, fige tout). Sans cette séparation, Q3 (« pas
  de présence après la fin ») et Q12 (« dépôt possible après la fin ») se
  contredisent en apparence. C'est l'hypothèse H1.

- **~10 min sur Q13.** « Remplacer le lien tant que personne n'a commencé à
  relire » : si le relecteur est tiré au dépôt, la relecture est assignée
  immédiatement et Q13 ne s'appliquerait jamais. J'ai donc interprété « commencé
  à relire » par « la relecture a été **rendue** », sinon la réponse du client est
  vide de sens (H4).

**IA :** je lui ai demandé un premier jet du cahier des charges à partir du sujet
et des 16 réponses, puis le découpage en tickets, puis le contrat OpenAPI.

Comment j'ai vérifié :

1. **Le contrat, par script et pas à l'œil.** Le vrai risque avec l'IA ici était
   qu'elle « améliore » les opérations imposées — corriger `400 CODE_INCONNU` en
   `404`, qui est le réflexe REST normal, aurait coûté la conformité au contrat.
   J'ai écrit un comparateur qui charge le `contrat.yaml` remis et le mien, et
   vérifie pour les 5 opérations : chemin, verbe, ensemble des codes de statut,
   champs `required` du corps et champs `required` de chaque réponse. Sortie :
   les 5 opérations passent. Le fichier est aussi validé comme YAML parsable.
2. **Les sources, une par une.** J'ai repris les 16 questions de `CLIENT.md` et
   vérifié que chacune était soit citée dans une règle de gestion, soit écartée
   volontairement. Q1 à Q16 : toutes tracées. Inversement, chaque RG cite un `Qx`
   ou un `Hx` — aucune règle ne sort de nulle part.
3. **Le backlog, au test du client.** L'IA proposait des tickets techniques
   (« créer l'entité Session », « configurer Flyway »). Je les ai écartés : un
   ticket doit être un résultat que le client comprend. Je suis reparti des
   exigences fonctionnelles, une issue par EF, douze au total. Les tâches
   techniques deviennent des sous-tâches de ces issues, pas des tickets.
4. **Les diagrammes contre le contrat.** D3 a été relu ligne à ligne pour que ses
   codes HTTP soient exactement ceux de `POST /api/presences` (201, 400, 409,
   410) ; D2 porte les contraintes `UNIQUE` qui produisent les 409.

Ce que l'IA n'a pas trouvé seule et que j'ai dû trancher : la portée du blocage de
Q4 (l'étudiant, faute d'authentification — H9), le statut HTTP de ce blocage
(400 et non 429, parce que le contrat imposé ne l'autorise pas — H7), et le cas
où l'auteur est le seul présent, donc sans relecteur possible (H3).

---

## Étape 2 — Première version

**Fait :** les sept exigences **Must** (EF1 à EF7), plus EF12 arrivée en bonus
parce que le formateur avait besoin de retrouver une séance pour la clôturer.
Une branche et une pull request par issue, le commit de fusion fermant l'issue.
Backend Spring Boot 3 / Java 17 avec `mvnw` committé, schéma Flyway V1 conforme à
D2, gestion centralisée des erreurs, 76 tests au vert dont six classes
d'intégration qui tournent sans base de données locale. Frontend React + Vite,
les trois écrans de F2, tous les appels réseau dans une seule couche.
`docker compose up` démarre l'ensemble avec les données de démonstration.

**Bloqué :**

- **~10 min, et le plus dangereux : mes tests d'intégration ne tournaient pas.**
  Surefire n'exécute pas les classes `*IT.java` — elles reviennent à Failsafe,
  lié à `verify` et non à `test`. `./mvnw test` affichait donc un vert parfait
  en ignorant **en silence** le seul test qui vérifiait la conformité au
  contrat. Repéré en comptant les classes exécutées dans la sortie, pas en
  lisant « BUILD SUCCESS ». Corrigé par une configuration explicite des
  `includes`. Leçon retenue pour le reste de la journée : un build vert ne dit
  pas *ce qui* a tourné.
- **~15 min sur un trou que l'analyse n'avait pas vu (H12).** Le corps imposé de
  `POST /api/relectures/{id}` est `{ note, commentaire }` — sans identité de
  l'appelant. Or le contrat exige un `403 AUTO_RELECTURE`, qui devient
  inatteignable, d'autant que le tirage interdit déjà d'assigner l'auteur.
  Tranché : un champ **facultatif** `relecteurId`, l'ensemble `required` du
  contrat restant inchangé. Ajouté en section 7 ; le cahier des charges bouge
  quand la réalité le contredit.
- **~5 min, deux fichiers ignorés en silence.** Mon fichier de données de
  démonstration s'appelait `D1__donnees.sql` : le préfixe Flyway par défaut est
  `V`, il n'a jamais été joué. Et `tsconfig.tsbuildinfo`, régénéré à chaque
  build, était suivi par Git. Deux erreurs sans message d'erreur, ce qui est la
  pire espèce.

**IA :** je lui ai demandé la traduction du cahier des charges en code —
entités, services, contrôleurs, tests — exigence par exigence, une branche à la
fois.

Comment j'ai vérifié :

1. **La conformité au contrat, par un test et non par relecture.** Chaque
   opération imposée a son test d'intégration qui assert sur le code HTTP **et**
   sur le champ `code` du corps d'erreur. Le piège que je surveillais est celui
   que l'IA tombe naturellement : « corriger » `400 CODE_INCONNU` en `404`,
   parce que c'est le réflexe REST normal. Le test l'interdit.
2. **J'ai compté les tests exécutés**, pas seulement lu « BUILD SUCCESS ».
   C'est ce qui a révélé le problème Surefire.
3. **J'ai relu chaque règle de gestion contre son test.** Les tests portent le
   nom de la règle (`rg2_codeExpireApresQuinzeMinutes`), donc l'absence se voit.
   Deux cas que l'IA n'avait pas couverts spontanément et que j'ai ajoutés :
   la quinzième minute **pile** (borne incluse ou exclue ?) et l'ordre des
   vérifications quand une séance est à la fois clôturée et expirée.
4. **J'ai fait tourner `docker compose` depuis un clone vierge**, pas seulement
   depuis mon dossier de travail, pour que le README dise la vérité.

Ce que j'ai refusé de ce qu'elle proposait : arrondir une note décimale au lieu
de la rejeter. Q9 dit « en nombres entiers » — arrondir, c'est inventer une
règle que le client n'a pas donnée.

---

## Étape 3 — Enveloppe

**Fait :**

**Bloqué :**

**IA :**

**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :**

---

## Étape 4 — Version finale

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 5 — Épreuve Git

**Fait :**

**Bloqué :**

**IA :**

---

## Étape 6 — Soumission

**Fait :**

**Ce que je referais autrement avec une journée de plus :**
