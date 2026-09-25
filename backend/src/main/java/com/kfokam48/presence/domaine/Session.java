package com.kfokam48.presence.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Une séance de cours.
 *
 * <p><strong>Trois notions distinctes, et c'est le cœur de l'analyse</strong>
 * (hypothèse H1 du cahier des charges) :
 * <ul>
 *   <li>l'<em>expiration du code</em>, 15 minutes après l'ouverture (RG2) : elle
 *       bloque les présences, mais pas les dépôts d'exercice (RG12) ;</li>
 *   <li>la <em>clôture</em> par le formateur (RG20) : irréversible, elle fige
 *       tout — présences, dépôts, liens et notes.</li>
 * </ul>
 * Sans cette distinction, Q3 et Q12 se contredisent en apparence.
 */
@Entity
@Table(name = "session")
public class Session {

    /** RG2 (Q2) — la durée de validité du code, unique endroit où ce chiffre est écrit. */
    public static final Duration VALIDITE_DU_CODE = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, unique = true, length = 6)
    private String code;

    @Column(name = "ouverture_at", nullable = false)
    private Instant ouvertureAt;

    @Column(name = "expiration_at", nullable = false)
    private Instant expirationAt;

    @Column(name = "cloture_at")
    private Instant clotureAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formateur_id")
    private Utilisateur formateur;

    protected Session() {}

    public Session(String titre, String code, Instant ouvertureAt, Promotion promotion, Utilisateur formateur) {
        this.titre = titre;
        this.code = code;
        this.ouvertureAt = ouvertureAt;
        this.expirationAt = ouvertureAt.plus(VALIDITE_DU_CODE);
        this.promotion = promotion;
        this.formateur = formateur;
    }

    /** RG20 — irréversible. Une seconde clôture est refusée par le service. */
    public void cloturer(Instant maintenant) {
        this.clotureAt = maintenant;
    }

    public boolean estCloturee() {
        return clotureAt != null;
    }

    /** RG2 — vrai dès que le code ne permet plus de marquer sa présence. */
    public boolean codeExpireA(Instant maintenant) {
        return !maintenant.isBefore(expirationAt);
    }

    public EtatSession etatA(Instant maintenant) {
        if (estCloturee()) {
            return EtatSession.CLOTUREE;
        }
        return codeExpireA(maintenant) ? EtatSession.EXPIREE : EtatSession.OUVERTE;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getCode() {
        return code;
    }

    public Instant getOuvertureAt() {
        return ouvertureAt;
    }

    public Instant getExpirationAt() {
        return expirationAt;
    }

    public Instant getClotureAt() {
        return clotureAt;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public Utilisateur getFormateur() {
        return formateur;
    }
}
