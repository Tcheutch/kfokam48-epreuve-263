package com.kfokam48.presence.domaine;

/**
 * Les trois états que le client confond en parlant de « fin de session » (H1).
 */
public enum EtatSession {
    /** Le code accepte encore des présences. */
    OUVERTE,
    /** Le code ne marche plus (RG2), mais les dépôts restent possibles (RG12). */
    EXPIREE,
    /** Le formateur a clôturé : tout est figé (RG20). */
    CLOTUREE
}
