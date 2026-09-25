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
  if (lignes.length === 0) return <p>Aucun étudiant dans cette promotion.</p>

  return (
    <table>
      <thead>
        <tr>
          <th align="left">Étudiant</th>
          <th align="right">Présences</th>
          <th align="right">Exercices déposés</th>
          <th align="right">Moyenne</th>
          <th align="right">Relectures à rendre</th>
        </tr>
      </thead>
      <tbody>
        {lignes.map((ligne) => (
          <tr key={ligne.etudiantId}>
            <td>{ligne.nom}</td>
            <td align="right">{ligne.presences}</td>
            <td align="right">{ligne.exercicesDeposes}</td>
            {/* null ne veut pas dire zéro : personne ne l'a encore noté. */}
            <td align="right">{ligne.moyenne === null ? '—' : ligne.moyenne}</td>
            <td align="right">
              {ligne.relecturesEnAttente > 0 ? (
                <strong title="En attente (Q11)">{ligne.relecturesEnAttente}</strong>
              ) : (
                0
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
