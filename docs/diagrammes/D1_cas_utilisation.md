# D1 — Cas d'utilisation

Acteurs et frontière du système. Chaque cas d'utilisation renvoie à l'exigence
fonctionnelle correspondante du [cahier des charges](../CAHIER_DES_CHARGES.md).

```mermaid
flowchart LR
    F(("Formateur"))
    E(("Étudiant"))
    R(("Relecteur"))

    subgraph SYS["Système — KFOKAM48 Présence &amp; Relecture"]
        UC1(["UC1 · Ouvrir une session et obtenir un code — EF1"])
        UC2(["UC2 · Clôturer la session — EF6"])
        UC3(["UC3 · Consulter le tableau de la promotion — EF7"])
        UC4(["UC4 · Ajouter une présence à la main — EF8"])
        UC5(["UC5 · Marquer sa présence avec le code — EF2"])
        UC6(["UC6 · Déposer ou remplacer le lien de son exercice — EF3"])
        UC7(["UC7 · Consulter la note et le commentaire reçus — EF9"])
        UC8(["UC8 · Rendre une relecture (note + commentaire) — EF5"])
        UC9(["UC9 · Corriger sa relecture avant clôture — EF5"])
        UC10(["UC10 · Assigner un relecteur au hasard — EF4"])
    end

    F --- UC1
    F --- UC2
    F --- UC3
    F --- UC4

    E --- UC5
    E --- UC6
    E --- UC7

    R --- UC8
    R --- UC9

    UC6 -. "«include»" .-> UC10
    UC10 -. "tire au sort parmi les présents, jamais l'auteur (RG5, RG7)" .-> R
    E -. "généralisation : un étudiant présent peut être désigné relecteur" .-> R
```

**Lecture.** Le relecteur n'est pas un acteur distinct mais un **rôle temporaire**
porté par un étudiant présent à la session (section 2 du cahier des charges) :
d'où la généralisation `Étudiant → Relecteur` plutôt qu'un troisième acteur
indépendant. Conséquence sur D2 : aucune table `Relecteur`, seulement une
association `Relecture.relecteurId → Utilisateur.id`.
