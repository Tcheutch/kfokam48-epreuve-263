import { useEffect, useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { LigneTableau } from '../api/types'
import { Chargement, Erreur } from './Etat'

/**
 * EF7 — le tableau du formateur (Q16).
 *
 * Ce composant **n'effectue aucun calcul**. La moyenne, les comptes et les
 * relectures dues arrivent de l'API (RG19, contrainte F3) : chercher ici une
 * division serait le signe qu'une règle a été dupliquée.
 *
 * Sous 640 px, chaque ligne devient une carte empilée. La transformation est
 * entièrement en CSS : la sémantique `<table>` est conservée, et les libellés
 * de colonne réapparaissent via `data-label` plutôt que par un second
 * balisage qu'il faudrait maintenir en double.
 */
export function TableauPromotion({ promotionId }: { promotionId: number }) {
  const [lignes, setLignes] = useState<LigneTableau[] | null>(null)
  const [erreur, setErreur] = useState<ErreurApi | null>(null)

  useEffect(() => {
    setLignes(null)
    setErreur(null)
    api
      .tableau(promotionId)
      .then(setLignes)
      .catch((e: ErreurApi) => setErreur(e))
  }, [promotionId])

  if (erreur) return <Erreur message={erreur.message} code={erreur.code} />
  if (!lignes) return <Chargement quoi="du tableau" />
  if (lignes.length === 0) return <p className="vide">Aucun étudiant dans cette promotion.</p>

  return (
    <div className="tableau-enveloppe">
      <table className="tableau">
        <caption className="etat-detail">
          Une ligne par étudiant de la promotion, y compris ceux qui n'ont encore rien fait.
        </caption>
        <thead>
          <tr>
            <th scope="col">Étudiant</th>
            <th scope="col" className="numerique">
              Présences
            </th>
            <th scope="col" className="numerique">
              Exercices déposés
            </th>
            <th scope="col" className="numerique">
              Moyenne
            </th>
            <th scope="col" className="numerique">
              Relectures à rendre
            </th>
          </tr>
        </thead>
        <tbody>
          {lignes.map((ligne) => (
            <tr key={ligne.etudiantId}>
              <td data-role="titre">{ligne.nom}</td>
              <td data-label="Présences" className="numerique">
                <Mesure valeur={ligne.presences} />
              </td>
              <td data-label="Exercices déposés" className="numerique">
                <Mesure valeur={ligne.exercicesDeposes} />
              </td>
              <td data-label="Moyenne" className="numerique">
                {/* null ne veut pas dire zéro : personne ne l'a encore noté. */}
                {ligne.moyenne === null ? (
                  <span className="valeur-vide" title="Aucune note reçue pour l'instant">
                    —
                  </span>
                ) : (
                  <span className="valeur">{ligne.moyenne}</span>
                )}
              </td>
              <td data-label="Relectures à rendre" className="numerique">
                {ligne.relecturesEnAttente > 0 ? (
                  <span className="valeur valeur--attention" title="En attente (Q11)">
                    {ligne.relecturesEnAttente}
                  </span>
                ) : (
                  <span className="valeur-vide">0</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** Un zéro se lit « rien pour l'instant », pas « erreur » : il s'atténue. */
function Mesure({ valeur }: { valeur: number }) {
  return valeur === 0 ? (
    <span className="valeur-vide">0</span>
  ) : (
    <span className="valeur">{valeur}</span>
  )
}
