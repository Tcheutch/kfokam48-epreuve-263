package com.kfokam48.presence.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * RG4 (Q4) — cinq codes erronés d'affilée valent deux minutes de blocage.
 *
 * <p>Le compteur est porté par l'étudiant et non par l'adresse IP : sans compte
 * ni session HTTP (Q1), c'est la seule granularité disponible (H9). Il est
 * persisté, et non gardé en mémoire : sinon la règle se contourne en
 * redémarrant le serveur.
 */
@Entity
@Table(name = "tentative_code")
public class TentativeCode {

    public static final int ECHECS_AVANT_BLOCAGE = 5;
    public static final Duration DUREE_DU_BLOCAGE = Duration.ofMinutes(2);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etudiant_id", nullable = false, unique = true)
    private Utilisateur etudiant;

    @Column(name = "echecs_consecutifs", nullable = false)
    private int echecsConsecutifs;

    @Column(name = "bloque_jusqua")
    private Instant bloqueJusqua;

    protected TentativeCode() {}

    public TentativeCode(Utilisateur etudiant) {
        this.etudiant = etudiant;
        this.echecsConsecutifs = 0;
    }

    public boolean estBloqueA(Instant maintenant) {
        return bloqueJusqua != null && maintenant.isBefore(bloqueJusqua);
    }

    /** Renvoie vrai si cet échec déclenche le blocage. */
    public boolean enregistrerEchec(Instant maintenant) {
        echecsConsecutifs++;
        if (echecsConsecutifs >= ECHECS_AVANT_BLOCAGE) {
            bloqueJusqua = maintenant.plus(DUREE_DU_BLOCAGE);
            echecsConsecutifs = 0;
            return true;
        }
        return false;
    }

    /** Un succès remet le compteur à zéro (Q4). */
    public void enregistrerSucces() {
        echecsConsecutifs = 0;
        bloqueJusqua = null;
    }

    public Long getId() {
        return id;
    }

    public Utilisateur getEtudiant() {
        return etudiant;
    }

    public int getEchecsConsecutifs() {
        return echecsConsecutifs;
    }

    public Instant getBloqueJusqua() {
        return bloqueJusqua;
    }
}
