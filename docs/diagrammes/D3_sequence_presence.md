# D3 — Séquence : marquer sa présence

Cas nominal et cas d'erreur. Les codes de statut et les codes d'erreur repris ici
sont **exactement** ceux de `api/contrat.yaml` pour `POST /api/presences`
(`201` · `400` · `409` · `410`).

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (React)
    participant C as PresenceController
    participant S as PresenceService
    participant R as Repositories

    E->>F: saisit le code de présence
    F->>C: POST /api/presences { code, etudiantId }
    C->>C: validation du corps (@Valid)
    C->>S: enregistrer(code, etudiantId)
    S->>R: tentatives.findByEtudiant(etudiantId)
    R-->>S: compteur d'échecs
    S->>R: sessions.findByCode(code)
    R-->>S: Session ou vide

    alt Étudiant bloqué — 5 échecs en moins de 2 min (RG4)
        S-->>C: TropDeTentativesException
        C-->>F: 400 { "code": "TROP_DE_TENTATIVES", "message": "Trop d'essais. Réessayez dans 2 minutes." }
    else Code inconnu (RG17)
        S->>R: tentatives.incrementer(etudiantId)
        S-->>C: CodeInconnuException
        C-->>F: 400 { "code": "CODE_INCONNU", "message": "Ce code de présence n'existe pas." }
    else Code expiré ou session clôturée (RG2, RG3, RG20)
        S-->>C: CodeExpireException
        C-->>F: 410 { "code": "CODE_EXPIRE", "message": "Le code de présence a expiré." }
    else Présence déjà enregistrée (RG15)
        S->>R: presences.existsBySessionAndEtudiant(...)
        R-->>S: true
        S-->>C: DejaPresentException
        C-->>F: 409 { "code": "DEJA_PRESENT", "message": "Votre présence est déjà enregistrée pour cette session." }
    else Cas nominal
        S->>R: presences.existsBySessionAndEtudiant(...)
        R-->>S: false
        S->>R: tentatives.remettreAZero(etudiantId)
        S->>R: presences.save(session, etudiant, source = ETUDIANT)
        R-->>S: Presence
        S-->>C: PresenceDto
        C-->>F: 201 { id, sessionId, etudiantId, source: "ETUDIANT" }
        F-->>E: confirmation à l'écran
    end
```

## Correspondance avec le contrat

| Situation | Règle | Statut HTTP | `code` d'erreur |
|---|---|---|---|
| Présence enregistrée | EF2 | `201` | — |
| Corps invalide (`code` ou `etudiantId` manquant) | B4 | `400` | `CHAMP_MANQUANT` |
| Code inexistant | RG17 | `400` | `CODE_INCONNU` |
| Trop de tentatives | RG4 | `400` | `TROP_DE_TENTATIVES` |
| Code expiré (> 15 min) ou session clôturée | RG2, RG3, RG20 | `410` | `CODE_EXPIRE` |
| Étudiant déjà présent | RG15 | `409` | `DEJA_PRESENT` |

> **Pourquoi `400` et non `429` pour RG4 ?** Le contrat est imposé : les seuls
> statuts autorisés sur `POST /api/presences` sont `400`, `409` et `410`.
> Ajouter un `429` sur une opération imposée sortirait du contrat. Le blocage est
> donc signalé par un `400` porteur du code métier `TROP_DE_TENTATIVES`.
> Décision tracée en section 7 du cahier des charges.

> **Pourquoi `400` et non `404` pour un code inconnu ?** C'est ce qu'impose le
> contrat : la ressource créée est la présence, pas la session ; le code est une
> donnée d'entrée invalide, donc `400`.
