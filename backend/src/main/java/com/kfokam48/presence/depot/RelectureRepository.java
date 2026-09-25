package com.kfokam48.presence.depot;

import com.kfokam48.presence.domaine.Relecture;
import com.kfokam48.presence.domaine.StatutRelecture;
import com.kfokam48.presence.service.projection.Comptage;
import com.kfokam48.presence.service.projection.Moyenne;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    Optional<Relecture> findByExerciceId(Long exerciceId);

    /**
     * RG11 — les relectures d'un étudiant, pour l'écran relecteur.
     *
     * <p>Le {@code join fetch} n'est pas une optimisation, c'est une
     * <strong>correction</strong>. La conversion en DTO a lieu dans le
     * contrôleur, donc <em>après</em> la fermeture de la session Hibernate
     * ({@code open-in-view: false}). Sans ce chargement immédiat, la traversée
     * {@code relecture -> exercice -> session} et {@code exercice -> etudiant}
     * lève une {@code LazyInitializationException} que le filet de sécurité
     * transforme en {@code 500} — l'écran relecteur devient inutilisable.
     *
     * <p>Corrigé ainsi plutôt qu'en activant {@code open-in-view}, qui aurait
     * masqué le problème au lieu de le résoudre : garder la session ouverte
     * pendant le rendu, c'est déplacer les requêtes hors de la couche qui les
     * contrôle. Au passage, une seule requête remplace les quatre qu'aurait
     * faites le chargement paresseux.
     */
    @Query("""
            select r
            from Relecture r
              join fetch r.exercice e
              join fetch e.session
              join fetch e.etudiant
            where r.relecteur.id = :relecteurId
            order by r.assigneeAt desc
            """)
    List<Relecture> findByRelecteurIdOrderByAssigneeAtDesc(Long relecteurId);

    /** RG11 — ce que compte {@code relecturesEnAttente} dans le tableau. */
    long countByRelecteurIdAndStatut(Long relecteurId, StatutRelecture statut);

    /**
     * RG19 — la moyenne des notes reçues par un étudiant, calculée par l'API et
     * nulle part ailleurs. Seules les relectures rendues comptent.
     */
    @Query("""
            select avg(r.note)
            from Relecture r
            where r.exercice.etudiant.id = :etudiantId
              and r.statut = com.kfokam48.presence.domaine.StatutRelecture.RENDUE
            """)
    Double moyenneDesNotesRecues(Long etudiantId);

    /**
     * EF7 · RG19 — la moyenne des notes reçues, par étudiant de la promotion.
     *
     * <p>Seules les relectures RENDUES comptent : une relecture assignée mais
     * jamais rendue ne doit pas tirer la moyenne vers le bas (Q11). Un étudiant
     * sans aucune note n'apparaît pas dans le résultat — sa moyenne est nulle,
     * et non zéro. Confondre les deux serait dire qu'il a eu zéro.
     */
    @Query("""
            select new com.kfokam48.presence.service.projection.Moyenne(
                r.exercice.etudiant.id, avg(r.note))
            from Relecture r
            where r.exercice.etudiant.promotion.id = :promotionId
              and r.statut = com.kfokam48.presence.domaine.StatutRelecture.RENDUE
            group by r.exercice.etudiant.id
            """)
    List<Moyenne> moyennesParEtudiant(Long promotionId);

    /** EF7 · RG11 — ce que chacun doit encore relire. */
    @Query("""
            select new com.kfokam48.presence.service.projection.Comptage(r.relecteur.id, count(r))
            from Relecture r
            where r.relecteur.promotion.id = :promotionId
              and r.statut = com.kfokam48.presence.domaine.StatutRelecture.ASSIGNEE
            group by r.relecteur.id
            """)
    List<Comptage> relecturesEnAttenteParEtudiant(Long promotionId);
}
