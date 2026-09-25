# Cahier des charges — KFOKAM48 Présence & Relecture

**Auteur :** Tcheutch · matricule 263
**Version :** 1 · **Date :** 25 septembre 2026
**Frontend choisi :** React, parce que son découpage en composants et ses hooks
d'état couvrent directement les trois écrans demandés et la gestion explicite des
états de chargement et d'erreur exigée par F3, sans le poids d'un framework
complet pour une application de cette taille.

---

## 1. Contexte et objectif

La direction de la formation KFOKAM48 suit aujourd'hui à la main deux choses qui
lui coûtent du temps : l'appel en début de cours et la correction des exercices.
L'appel sur papier se perd, se falsifie et n'est consolidé nulle part ; la
correction, elle, ne passe pas à l'échelle d'une promotion entière.

L'application répond à ces deux problèmes. Le formateur ouvre une session de
cours et obtient un code de présence à durée de vie courte ; les étudiants
saisissent ce code depuis leur téléphone, ce qui constitue l'appel sans
intervention du formateur. Chaque étudiant dépose ensuite le lien de son
exercice, et le système désigne au hasard un pair pour le relire et le noter.

L'objectif mesurable est unique : donner au formateur, à tout moment et pour une
promotion donnée, **un tableau consolidé** — présences, exercices déposés,
moyenne des notes reçues, relectures encore dues. C'est ce tableau qui est la
finalité de l'application ; tout le reste sert à l'alimenter.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| **Formateur** | Ouvrir une session et obtenir son code (EF1) · clôturer la session (EF6) · ajouter une présence à la main, tracée `source = FORMATEUR` (EF8) · consulter le tableau de sa promotion (EF7) | Saisir, modifier ou supprimer une note ou un commentaire de relecture · rouvrir une session clôturée (RG20) |
| **Étudiant** | Marquer sa présence avec le code (EF2) · déposer puis remplacer le lien de son exercice (EF3) · consulter la note et le commentaire reçus (EF9) | Marquer sa présence après expiration du code ou clôture de la session (RG2, RG3) · connaître l'identité de son relecteur (RG8) · déposer deux exercices pour une même session (RG16) |
| **Relecteur** | Rendre une note entière et un commentaire sur l'exercice qui lui est assigné (EF5) · corriger cette relecture tant que la session est ouverte (RG10) | Choisir l'exercice qu'il relit (le tirage est fait par le système, RG7) · relire son propre exercice (RG5) · modifier sa note après clôture (RG20) |

**Le relecteur n'est pas un acteur distinct.** C'est un **état temporaire d'un
étudiant** : tout étudiant présent à une session est éligible au tirage au sort,
et le devient effectivement quand le système lui assigne un exercice.

*Conséquence assumée sur le modèle de données (D2)* : pas de table `Relecteur`,
pas de rôle supplémentaire dans `utilisateur`. Le rôle se lit dans l'existence
d'une ligne `relecture` dont le `relecteur_id` pointe vers cet étudiant. Un même
étudiant est donc simultanément auteur d'un exercice et relecteur d'un autre.

## 3. Périmètre

**Inclus dans cette version :**
- Ouverture d'une session par un formateur, avec génération d'un code de présence expirant en 15 minutes.
- Marquage de présence par code, avec anti-force-brute (blocage 2 minutes après 5 échecs).
- Ajout manuel d'une présence par le formateur, distinguable de celles marquées par les étudiants.
- Clôture explicite d'une session par le formateur, qui fige les dépôts et les notes.
- Dépôt et remplacement du lien d'un exercice.
- Tirage au sort d'un relecteur unique parmi les étudiants présents, hors auteur.
- Notation entière sur 20 et commentaire, avec correction possible avant clôture.
- Restitution anonyme de la note et du commentaire à l'étudiant relu.
- Tableau récapitulatif par promotion : présences, dépôts, moyenne, relectures en attente.
- Jeu de données de démonstration chargé au démarrage.

**Explicitement exclu :**
- **Authentification et mots de passe** (Q1) : l'utilisateur se choisit dans une liste. Aucune session HTTP, aucun jeton, aucune autorisation serveur — l'`etudiantId` est envoyé par le client et pris pour argent comptant. C'est un choix assumé du client, pas un oubli ; l'application n'est pas exposée sur Internet.
- **Téléversement de fichiers** : seuls des liens URL sont stockés, jamais le contenu des exercices.
- **Notifications** (courriel, SMS, push) : le relecteur découvre sa relecture en ouvrant son écran.
- **Temps réel** : pas de WebSocket ; les écrans se rafraîchissent au chargement ou à la demande.
- **Second relecteur, arbitrage, contestation d'une note** (Q6 fixe un relecteur unique).
- **Suppression ou modification d'une session après ouverture** (hors clôture).
- **Soin apporté au CSS** : le rendu visuel n'est pas évalué, la mise en forme reste minimale et fonctionnelle.
- **Statistiques historiques, export CSV/PDF, multi-promotion dans un même tableau.**

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| **EF1** | Le formateur ouvre une session de cours et obtient un code de présence | Quand je saisis un titre et une promotion existante, alors je reçois `201` avec un `code`, une `ouvertureAt` et une `expirationAt` fixée 15 minutes plus tard ; si le titre ou la promotion manque, alors je reçois `400 CHAMP_MANQUANT` | Must |
| **EF2** | L'étudiant marque sa présence à l'aide du code | Quand je saisis un code valide et non expiré et que je n'ai pas déjà été marqué, alors je reçois `201` et ma présence apparaît dans le tableau du formateur avec `source = ETUDIANT` | Must |
| **EF3** | L'étudiant dépose le lien de son exercice pour une session | Quand je soumets une URL `http(s)` absolue pour une session non clôturée où je n'ai rien déposé, alors je reçois `201 { id, statut: "DEPOSE" }` ; si l'URL est malformée, alors `400 LIEN_INVALIDE` | Must |
| **EF4** | Le système assigne au hasard un relecteur à chaque exercice déposé | Quand un exercice est déposé et qu'au moins un autre étudiant est présent à la session, alors une relecture est créée au statut `ASSIGNEE` pour un étudiant tiré au sort parmi les présents, jamais l'auteur | Must |
| **EF5** | Le relecteur rend une note et un commentaire | Quand je soumets une note entière entre 0 et 20 et un commentaire sur la relecture qui m'est assignée, alors je reçois `200` et la note entre dans la moyenne de l'auteur ; une note décimale ou hors bornes renvoie `400 NOTE_INVALIDE` | Must |
| **EF6** | Le formateur clôture une session | Quand je clôture une session ouverte, alors tout dépôt, remplacement de lien, marquage de présence ou correction de note ultérieur est refusé par un `409 SESSION_CLOTUREE` | Must |
| **EF7** | Le formateur consulte le tableau de sa promotion | Quand je sélectionne une promotion existante, alors j'obtiens `200` avec une ligne par étudiant portant `presences`, `exercicesDeposes`, `moyenne` (nulle si aucune note reçue) et `relecturesEnAttente` ; une promotion inconnue renvoie `404 PROMOTION_INCONNUE` | Must |
| **EF8** | Le formateur ajoute une présence à la main | Quand j'ajoute un étudiant absent de la liste des présents, alors sa présence est créée avec `source = FORMATEUR` et le tableau l'affiche comme « ajoutée par le formateur » | Should |
| **EF9** | L'étudiant relu consulte sa note et son commentaire | Quand ma relecture a été rendue, alors je vois la note et le commentaire, et aucun champ de la réponse ne permet d'identifier le relecteur | Should |
| **EF10** | Le système limite les essais de code | Quand j'échoue 5 fois d'affilée sur un code, alors mes 2 minutes suivantes sont refusées par un `400 TROP_DE_TENTATIVES`, et un succès remet le compteur à zéro | Should |
| **EF11** | Le relecteur corrige une relecture déjà rendue | Quand je resoumets une note sur une relecture déjà rendue et que la session est encore ouverte, alors je reçois `200` et la moyenne est recalculée | Should |
| **EF12** | Le formateur liste les sessions de la promotion | Quand j'ouvre l'écran formateur, alors je vois les sessions de la promotion avec leur état (ouverte / expirée / clôturée) | Could |

> Les exigences **Must** constituent le périmètre du jalon `v0.1` (étape 2).
> Les **Should** sont livrées à l'étape 4 (`v1.0`). La **Could** saute en premier
> si l'enveloppe de l'étape 3 consomme le temps restant.

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| **ENF1** | L'écran de marquage de présence est utilisable sur un téléphone | Affichage et saisie du code sans défilement horizontal à 360 px de large (DevTools, profil mobile) |
| **ENF2** | Le tableau du formateur répond en moins de 2 s pour une promotion de 60 étudiants et 20 sessions | Jeu de démonstration chargé à cette volumétrie, mesure du temps de réponse de `GET /api/tableau` avec `curl -w "%{time_total}"` |
| **ENF3** | Volumétrie cible : 5 promotions, 60 étudiants par promotion, 20 sessions par promotion et par trimestre, soit ~6 000 présences et ~6 000 relectures par trimestre | Le schéma reste sur une base relationnelle unique ; aucun besoin de partitionnement à cet ordre de grandeur |
| **ENF4** | Aucune erreur ne sort au format brut | Toute réponse d'erreur, y compris `404` inattendu ou exception non prévue, renvoie `{ code, message }` en français ; vérifié par un test d'intégration sur un endpoint inexistant et par appel cURL |
| **ENF5** | Le schéma est reconstructible depuis zéro | `docker compose down -v && docker compose up` recrée la base et rejoue toutes les migrations Flyway sans intervention manuelle |
| **ENF6** | L'application démarre chez un tiers depuis le seul `README` | Clone vierge dans un dossier vide, au plus trois commandes, données de démonstration présentes à l'écran |
| **ENF7** | Les tests tournent sans base locale | `./mvnw test` passe sur un poste sans PostgreSQL : les tests d'intégration utilisent H2 en mémoire ou Testcontainers |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| **RG1** | Aucun mot de passe : l'utilisateur se choisit dans la liste de sa promotion | Q1 |
| **RG2** | Un code de présence expire 15 minutes après l'ouverture de la session | Q2 |
| **RG3** | Aucune présence ne peut être marquée après la fin de la session ; « fin de session » = expiration du code **ou** clôture par le formateur | Q3 + H1 |
| **RG4** | Après 5 échecs consécutifs sur un code, l'étudiant est bloqué 2 minutes ; un succès remet le compteur à zéro | Q4 |
| **RG5** | Un étudiant ne peut jamais relire son propre exercice | Q5 |
| **RG6** | Un exercice reçoit exactement un relecteur, jamais deux | Q6 |
| **RG7** | Le relecteur est tiré au sort par le système parmi les étudiants **présents à cette session**, l'auteur exclu | Q7 |
| **RG8** | L'étudiant relu voit la note et le commentaire, mais jamais l'identité du relecteur — aucun champ de l'API ne l'expose | Q8 |
| **RG9** | Une note est un **entier** compris entre 0 et 20 inclus ; une valeur décimale est refusée | Q9 |
| **RG10** | Le relecteur peut corriger sa note et son commentaire tant que la session n'est pas clôturée | Q10, retenu contre Q15 (section 7) |
| **RG11** | Une relecture assignée mais non rendue laisse l'exercice « en attente » et compte dans `relecturesEnAttente` du relecteur | Q11 |
| **RG12** | Le dépôt d'un exercice reste possible après l'expiration du code, jusqu'à la clôture de la session | Q12 |
| **RG13** | Le lien d'un exercice peut être remplacé tant que la relecture assignée n'a pas été **rendue** | Q13 + H4 |
| **RG14** | Toute présence porte une `source` : `ETUDIANT` si l'étudiant a saisi le code, `FORMATEUR` si le formateur l'a ajoutée à la main | Q14 |
| **RG15** | Au plus une présence par couple (session, étudiant) ; un second marquage renvoie `409 DEJA_PRESENT` | Contrat |
| **RG16** | Au plus un exercice par couple (session, étudiant) ; un second dépôt renvoie `409 EXERCICE_DEJA_DEPOSE` | Contrat |
| **RG17** | Un code de présence inconnu renvoie `400 CODE_INCONNU`, et non `404` | Contrat |
| **RG18** | Le lien d'un exercice est une URL absolue en `http` ou `https` ; sinon `400 LIEN_INVALIDE` | H5 |
| **RG19** | La `moyenne` d'un étudiant est la moyenne arithmétique des notes des relectures **rendues** portant sur ses exercices ; elle vaut `null` s'il n'en a reçu aucune, et n'est calculée que côté API | Q16, F3 |
| **RG20** | La clôture d'une session est irréversible : après elle, plus aucune présence, plus aucun dépôt, plus aucune modification de note | H1 |
| **RG21** | Le code de présence est unique parmi les sessions non expirées et non clôturées, et n'est pas devinable (6 caractères alphanumériques tirés d'un générateur sûr) | H6 |
| **RG22** | Un exercice déposé alors qu'aucun autre étudiant n'est présent reste au statut `DEPOSE` ; le tirage est rejoué à chaque nouvelle présence enregistrée sur la session | H3 |

> Ces références sont citées dans les titres d'issues, les messages de commit et
> les noms de tests (`PresenceServiceTest#rg2_codeExpireApres15Minutes`).

## 7. Zones d'ombre, hypothèses et contradictions

### Contradiction relevée

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| **Q10** — « un relecteur peut corriger sa note tant que le formateur n'a pas clôturé la session » **contre** **Q15** — « une fois que le relecteur a validé, c'est fini, il ne peut plus y revenir » | **Q10** : la correction est autorisée, et la clôture de la session est ce qui fige la note (RG10, RG20) | Q10 énonce une **règle opérationnelle bornée** — elle nomme un événement précis et vérifiable, la clôture, qui existe déjà dans la réponse Q12. Q15 énonce une **intention morale** (« c'est plus honnête »), sans borne technique. Entre une règle et une intention, on implémente la règle : elle est testable, l'autre non. Le choix de Q10 n'interdit d'ailleurs rien à Q15 — il suffit au formateur de clôturer sa session pour obtenir exactement le comportement voulu par Q15, alors que l'inverse serait impossible. Conséquence : `POST /api/relectures/{id}` est **idempotent tant que la session est ouverte** et ne renvoie `409 RELECTURE_DEJA_RENDUE` qu'après clôture (visible dans D4). |

### Le trou que personne n'avait vu

| Point | Constat | Décision retenue | Conséquence |
|---|---|---|---|
| **H1 — Rien ne permet de clôturer une session** | Q10 et Q12 conditionnent deux règles majeures à « ce que je clôture la session », mais **aucune des 5 opérations imposées ne clôture quoi que ce soit**, et le client ne définit nulle part ce que « clôturer » veut dire. Sans clôture, la note ne devient jamais définitive et le dépôt d'exercices ne s'arrête jamais. | Ajout de `POST /api/sessions/{id}/cloture` au contrat, d'un champ `session.cloture_at` nullable, d'EF6 et de RG20. La clôture est **irréversible** et distincte de l'expiration du code. | Trois notions désormais distinctes et nommées : **expiration du code** (15 min, bloque les présences), **fin de session** (RG3), **clôture** (irréversible, fige tout). Sans cette distinction, Q3 et Q12 se contredisent en apparence. |
| **H2 — Le contrat imposé ne permet pas la présence manuelle (Q14)** | `POST /api/presences` n'accepte que `{ code, etudiantId }` : aucun moyen d'y produire une présence `source = FORMATEUR`. Pourtant le champ `source` existe dans la réponse et Q14 exige la fonctionnalité. | Ajout de `POST /api/sessions/{id}/presences` (réservé au formateur), qui crée une présence `source = FORMATEUR` sans code. L'opération imposée reste inchangée et produit toujours `source = ETUDIANT`. | Le contrat imposé est respecté à la lettre ; la fonctionnalité Q14 vit dans une opération ajoutée, comme le contrat y autorise. |

### Autres points non tranchés par le client

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| **H3 — Aucun autre étudiant présent au moment du dépôt** | Q7 impose de tirer parmi les présents, Q5 interdit l'auto-relecture : si l'auteur est seul présent, l'ensemble des relecteurs éligibles est vide. Le client n'envisage pas ce cas. | L'exercice reste au statut `DEPOSE`, sans relecture. Le tirage est **rejoué à chaque nouvelle présence** enregistrée sur la session (RG22). | Le tableau distingue « déposé, sans relecteur » de « en attente de relecture » ; l'écran étudiant affiche l'état. Un exercice peut légitimement terminer sans note. |
| **H4 — Que veut dire « commencé à relire » (Q13) ?** | Si le tirage a lieu au dépôt, la relecture est assignée immédiatement et Q13 (« on peut remplacer le lien tant que personne n'a commencé ») ne pourrait jamais s'appliquer : la réponse serait vide de sens. | « Commencé à relire » = **la relecture a été rendue** (`statut = RENDUE`), et non « a été assignée ». Le lien reste donc remplaçable tant que la note n'est pas tombée (RG13). | Un relecteur peut voir le lien changer sous lui avant d'avoir rendu sa note. Accepté : l'alternative vide Q13 de son contenu. L'écran relecteur recharge le lien à chaque affichage. |
| **H5 — Qu'est-ce qu'un « lien invalide » ?** | Le contrat impose `400 LIEN_INVALIDE` sans définir la validité ; `format: uri` n'est pas contraignant. | URL absolue, schéma `http` ou `https`, longueur ≤ 2 048 caractères. Ni accessibilité ni domaine vérifiés — aucun appel réseau sortant depuis le backend. | Un lien mort est accepté ; le relecteur le signalera dans son commentaire. |
| **H6 — Forme du code de présence** | Non abordé. Q4 révèle en creux le risque : « sinon ils vont deviner les codes entre eux ». | 6 caractères alphanumériques majuscules, tirés d'un `SecureRandom`, sans caractères ambigus (`0/O`, `1/I`), uniques parmi les sessions actives (RG21). | Espace de ~1,7 milliard de combinaisons : le blocage de RG4 devient réellement dissuasif. |
| **H7 — Statut HTTP du blocage après 5 échecs (Q4)** | `429 Too Many Requests` serait le statut canonique, mais le contrat n'autorise que `400`, `409` et `410` sur `POST /api/presences`, et il est imposé « à la lettre ». | `400` porteur du code métier `TROP_DE_TENTATIVES`. | Le contrat reste intact ; le front distingue le cas par le champ `code`, pas par le statut. Arbitrage assumé entre orthodoxie HTTP et conformité au contrat — le contrat gagne, il est noté. |
| **H8 — L'anonymat du relecteur est-il réciproque ?** | Q8 ne protège que l'auteur : « pas le nom du relecteur ». Rien n'est dit sur ce que voit le relecteur. | **Anonymat simple, pas double** : le relecteur voit le nom de l'auteur (il est de toute façon souvent lisible dans le lien vers le dépôt). Seule l'identité du relecteur est masquée (RG8). | Décision inverse de ce qui pourrait sembler « plus propre », mais conforme à ce que le client a écrit et non à ce qu'on suppose qu'il voulait. |
| **H9 — Portée du blocage de RG4** | Q4 dit « bloquez-le » sans dire qui : l'étudiant, l'adresse IP, le code ? | Le compteur est porté par l'**étudiant** (`tentative_code.etudiant_id`), puisqu'il n'y a ni compte ni session HTTP (Q1). | Un étudiant bloqué le reste même en changeant d'appareil ; sans authentification, c'est la seule granularité disponible. Documenté comme limite. |
| **H10 — Un étudiant absent peut-il déposer un exercice ?** | Non tranché. Q12 parle du délai, pas de la condition de présence. | **Oui** : le dépôt n'exige pas la présence. Seul le tirage au sort exige la présence, et du côté du relecteur (Q7). | Un étudiant marqué absent peut déposer et être noté. Cohérent avec Q12 (« certains n'ont pas de connexion le soir même »). |
| **H11 — Que faire d'un `etudiantId` d'une autre promotion ?** | Non abordé. | `400 ETUDIANT_HORS_PROMOTION` : un étudiant ne peut marquer sa présence qu'à une session de sa propre promotion. | Évite qu'un code diffusé hors de la promotion soit exploitable. |

*Toute hypothèse écrite ici est assumée et tranchée. Aucune décision de ce
document ne repose sur une information qui n'est pas soit citée (`Qx`), soit
déclarée comme hypothèse (`Hx`).*

## 8. Contraintes techniques

### Imposées par le sujet

| Réf | Contrainte | Comment elle est tenue |
|---|---|---|
| **B1** | Java 17+, Maven, wrapper `mvnw` commité | Java 17, Spring Boot 3.x, `mvnw` et `.mvn/wrapper/` versionnés dès le premier commit de code |
| **B2** | `api/contrat.yaml` respecté à la lettre | Les 5 opérations imposées ne sont ni renommées ni enrichies d'un statut ; les opérations ajoutées le sont dans le même fichier (voir H1, H2) |
| **B3** | Séparation contrôleur / service / repository, DTO obligatoires | Aucune requête dans un contrôleur, aucune entité JPA dans une signature de contrôleur ; mapping entité → DTO explicite |
| **B4** | Validation des entrées et `@RestControllerAdvice` | `@Valid` sur tous les corps de requête ; un unique `GestionnaireErreurs` traduit chaque exception métier en `{ code, message }` ; `ErrorMvcAutoConfiguration` neutralisée pour supprimer la page d'erreur Spring |
| **B5** | Schéma versionné, `ddl-auto=update` interdit | Flyway, migrations `V1__schema_initial.sql`, `V2__…` ; `spring.jpa.hibernate.ddl-auto=validate` en développement et en production |
| **B6** | Un test unitaire métier + un test d'intégration | `PresenceServiceTest` (RG2 : expiration du code) et `PresenceControllerIT` (`POST /api/presences` : 201/400/409/410) ; H2 en mémoire, aucune base locale requise |
| **F1** | Framework déclaré et justifié, build qui passe | React justifié en tête de ce document et dans le `README` ; `npm run build` vérifié |
| **F2** | Trois écrans | Formateur (ouvrir une session, tableau, clôture) · Étudiant (présence, dépôt, note reçue) · Relecteur (rendre une relecture) |
| **F3** | Couche d'appel API dédiée, états gérés, aucune règle métier dupliquée | Un seul module `src/api/` ; chaque écran gère `chargement` / `erreur` / `données` ; la `moyenne` affichée vient de `GET /api/tableau`, jamais recalculée côté client |

### Que je m'impose en plus

- **Base de données :** PostgreSQL 16 en exécution (via `docker compose`), H2 en mémoire pour les tests. Aucun type propriétaire dans les migrations pour que les deux moteurs acceptent le même SQL.
- **Migrations :** une migration par changement de schéma, jamais modifiée après avoir été poussée. L'enveloppe de l'étape 3 touchera la base — c'est précisément ce que cette discipline protège.
- **Tests :** au-delà du minimum B6, un test par règle de gestion arbitrée (RG5, RG9, RG10, RG13, RG20), nommé d'après sa règle.
- **Données de démonstration :** chargées par une migration Flyway dédiée `V*__donnees_demo.sql`, à la volumétrie d'ENF2, pour que le correcteur n'ouvre pas une application vide.
- **`.gitignore` Java + JS posé avant le premier commit de code** ; `target/`, `node_modules/`, `dist/`, `.env` jamais versionnés. Aucun secret : la configuration sensible passe par variables d'environnement avec valeurs par défaut de développement.
- **Journalisation** des erreurs côté serveur avec l'identifiant de corrélation, jamais renvoyée au client.

## 9. Livrables

| Livrable | Emplacement | Jalon |
|---|---|---|
| Cahier des charges (ce document) | `docs/CAHIER_DES_CHARGES.md` | `[JALON] analyse`, mis à jour après l'étape 3 |
| Diagrammes D1 à D4, en Mermaid versionné | `docs/diagrammes/` | `[JALON] analyse`, mis à jour après l'étape 3 |
| Journal de bord, une entrée par étape | `docs/JOURNAL.md` | une entrée à la fin de chaque étape |
| Contrat d'API complet et figé | `api/contrat.yaml` | **avant le premier commit de code** |
| Backlog en issues, avec critères d'acceptation, priorité et renvoi `EFx`/`RGx` | issues du dépôt | `[JALON] analyse` |
| Backend Spring Boot, migrations Flyway, tests | `backend/` | `v0.1` puis `v1.0` |
| Frontend React, trois écrans | `frontend/` | `v0.1` puis `v1.0` |
| `README.md` d'installation, testé depuis un clone vierge | racine | `v1.0` |
| `CHANGELOG.md` cohérent avec l'historique Git | racine | `v1.0` |
| `docker-compose.yml` | racine | `v0.1` |
| `SOUMISSION.md` téléversé sur la plateforme | hors dépôt | étape 6 |

## 10. Démarche prévue

| Étape | Ce que je vise | Ce que je fais si je prends du retard |
|---|---|---|
| **1. Analyse** | Ce document, D1 à D4, le backlog en issues, `api/contrat.yaml` complété et figé. Puis `[JALON] analyse`, avant toute ligne de code. | Rien n'est sacrifié ici : c'est 38 points sur 100. Je réduis plutôt le produit. |
| **2. Première version** | Les seules stories **Must** (EF1 à EF7). Une branche par issue, une PR par branche, `Closes #n` dans le commit de fusion. `.gitignore` et migration initiale **avant** le premier code. Puis `[JALON] v0.1`. | Je livre EF1, EF2, EF3, EF7 (le cœur : session → présence → dépôt → tableau) et repousse EF4/EF5 en Should. |
| **3. Enveloppe** | Ouvrir une issue **avant** de coder, reproduire le bug par un test qui échoue, corriger dans une migration versionnée, mettre à jour `contrat.yaml`, puis ce document et les diagrammes dans un commit qui le dit. Correctif et évolution dans des commits séparés. | Je re-priorise par écrit dans `JOURNAL.md` et je sors les **Could** du périmètre. |
| **4. Version finale** | Les **Should**, `[JALON] v1.0`, `CHANGELOG.md`, `README` testé depuis un clone vierge dans un dossier vide, backlog restant trié et annoté. | Je préfère un périmètre réduit **annoncé** dans `SOUMISSION.md` à une promesse non tenue. |
| **5. Épreuve Git** | Dépôt séparé `kfokam48-gitlab-263`, les cinq situations résolues, toutes les branches poussées. ~20 minutes, jamais mélangé à l'historique du projet. | Cette étape vaut 17 points pour 20 minutes : elle passe **avant** tout polissage du produit. |
| **6. Soumission** | `SOUMISSION.md` rempli, deux liens vérifiés en navigation privée, hashes complets relevés **après** le dernier push. | Aucune marge : la plateforme ferme à 18h00, je vise 17h00. |

**Definition of Done — un ticket est terminé quand :**
- Les critères d'acceptation écrits dans l'issue sont vérifiés à la main sur l'application qui tourne.
- La règle de gestion citée dans l'issue (`RGx`) est couverte par un test automatisé qui la nomme.
- Le contrat d'API est respecté : chemin, verbe, statut et format `{ code, message }` sur **tous** les cas d'erreur, vérifiés à la main ou par test.
- Le frontend concerné gère explicitement l'état de chargement et l'état d'erreur.
- La branche est fusionnée sur `main` par une PR qui référence l'issue, `main` compile et ses tests passent.
- Si le schéma a bougé, la migration est versionnée **et** D2 est à jour dans le même commit.

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 25/09/2026 | Version initiale, après lecture du sujet et des 16 réponses de `CLIENT.md`. |

> *L'étape 3 rendra une partie de ce document faux. Il faudra revenir le corriger
> et l'inscrire ici, dans un commit qui le dit — un cahier des charges périmé est
> un cahier des charges mort.*
