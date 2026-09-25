package com.kfokam48.presence.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Créée dès le tirage, au statut {@code ASSIGNEE}, note nulle.
 *
 * <p>C'est ce que suppose le contrat imposé : {@code POST /api/relectures/{id}}
 * reçoit l'identifiant d'une relecture <em>existante</em>. C'est aussi ce qui
 * rend calculable le {@code relecturesEnAttente} du tableau (RG11, Q11).
 */
@Entity
@Table(name = "relecture")
public class Relecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercice_id", nullable = false, unique = true)
    private Exercice exercice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relecteur_id", nullable = false)
    private Utilisateur relecteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRelecture statut;

    private Integer note;

    @Column(length = 2000)
    private String commentaire;

    @Column(name = "assignee_at", nullable = false)
    private Instant assigneeAt;

    @Column(name = "rendue_at")
    private Instant rendueAt;

    protected Relecture() {}

    public Relecture(Exercice exercice, Utilisateur relecteur, Instant assigneeAt) {
        this.exercice = exercice;
        this.relecteur = relecteur;
        this.statut = StatutRelecture.ASSIGNEE;
        this.assigneeAt = assigneeAt;
    }

    /**
     * RG10 — rejouable tant que la session n'est pas clôturée. L'arbitrage
     * Q10 contre Q15 est justifié en section 7 du cahier des charges ; le
     * service refuse l'appel après clôture.
     */
    public void rendre(int note, String commentaire, Instant maintenant) {
        this.note = note;
        this.commentaire = commentaire;
        this.statut = StatutRelecture.RENDUE;
        this.rendueAt = maintenant;
    }

    public boolean estRendue() {
        return statut == StatutRelecture.RENDUE;
    }

    public Long getId() {
        return id;
    }

    public Exercice getExercice() {
        return exercice;
    }

    public Utilisateur getRelecteur() {
        return relecteur;
    }

    public StatutRelecture getStatut() {
        return statut;
    }

    public Integer getNote() {
        return note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public Instant getAssigneeAt() {
        return assigneeAt;
    }

    public Instant getRendueAt() {
        return rendueAt;
    }
}
