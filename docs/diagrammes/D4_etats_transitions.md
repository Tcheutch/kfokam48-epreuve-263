# D4 — États-transitions : cycle de vie d'un exercice

*Quatrième diagramme, facultatif (bonus +3 points du barème).*

Les trois états correspondent au champ `exercice.statut` de D2 et à la valeur
`statut` renvoyée par `POST /api/exercices` (`201 { id, statut }`).

```mermaid
stateDiagram-v2
    [*] --> DEPOSE : POST /api/exercices — l'étudiant dépose son lien (EF3)

    DEPOSE : DEPOSE
    DEPOSE : aucun relecteur disponible
    EN_ATTENTE : EN_ATTENTE
    EN_ATTENTE : relecture assignée, pas encore rendue
    RELU : RELU
    RELU : note et commentaire rendus

    DEPOSE --> DEPOSE : remplacement du lien (RG13)
    DEPOSE --> EN_ATTENTE : tirage au sort d'un relecteur présent, hors auteur (RG5, RG7)

    EN_ATTENTE --> EN_ATTENTE : remplacement du lien tant que la relecture n'est pas rendue (RG13)
    EN_ATTENTE --> RELU : POST /api/relectures/{id} — 200 (EF5)

    RELU --> RELU : le relecteur corrige sa note tant que la session est ouverte (RG10, Q10 retenu contre Q15)

    DEPOSE --> FIGE : clôture de la session par le formateur (RG20)
    EN_ATTENTE --> FIGE : clôture — l'exercice reste « en attente » et compte dans relecturesEnAttente (RG11)
    RELU --> FIGE : clôture — la note devient définitive

    FIGE : FIGE (session clôturée)
    FIGE : plus aucune modification possible
    FIGE --> [*]
```

## Lecture

| Transition | Déclencheur | Effet observable |
|---|---|---|
| `[*] → DEPOSE` | `POST /api/exercices` | `201 { id, statut: "DEPOSE" }` ; `exercicesDeposes` +1 dans le tableau |
| `DEPOSE → EN_ATTENTE` | tirage automatique d'un relecteur | une ligne `relecture` au statut `ASSIGNEE` ; `relecturesEnAttente` +1 pour le relecteur |
| `EN_ATTENTE → RELU` | `POST /api/relectures/{id}` | `relecture.statut = RENDUE` ; la note entre dans la `moyenne` de l'auteur |
| `RELU → RELU` | nouvel appel sur la même relecture, session ouverte | `200` ; la moyenne est recalculée |
| `* → FIGE` | clôture de la session | tout nouvel appel renvoie `409 SESSION_CLOTUREE` |

**Un exercice peut rester `DEPOSE`** : si aucun autre étudiant n'est présent à la
session, il n'existe aucun relecteur éligible (RG5 interdit l'auto-relecture) et
le tirage est rejoué à chaque nouvelle présence. Ce cas est documenté en
section 7 du cahier des charges.

**`RELU → RELU` est la conséquence directe** de l'arbitrage Q10 contre Q15
(section 7). Si l'arbitrage inverse avait été retenu, cette boucle disparaîtrait
et `POST /api/relectures/{id}` renverrait `409 RELECTURE_DEJA_RENDUE` dès le
second appel.
