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
import jakarta.persistence.Table;

/**
 * Formateur et étudiant partagent la même table : ils ont exactement les mêmes
 * attributs, et présence, exercice et relecture pointent toutes vers cette clé.
 * Voir D2, « choix de modélisation ».
 */
@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    protected Utilisateur() {}

    public Utilisateur(String nom, Role role, Promotion promotion) {
        this.nom = nom;
        this.role = role;
        this.promotion = promotion;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Role getRole() {
        return role;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public boolean estEtudiant() {
        return role == Role.ETUDIANT;
    }
}
