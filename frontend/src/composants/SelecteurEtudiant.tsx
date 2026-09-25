import { useEffect, useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { Promotion, Utilisateur } from '../api/types'
import { Chargement, Erreur } from './Etat'

/**
 * RG1 (Q1) — « l'étudiant choisit son nom dans une liste ». Pas de mot de
 * passe, pas de compte : ce sélecteur tient lieu d'identification, et il est
 * partagé par l'écran étudiant et l'écran relecteur.
 */
export function SelecteurEtudiant({
  etudiantId,
  onChange,
}: {
  etudiantId: number | ''
  onChange: (id: number | '') => void
}) {
  const [promotions, setPromotions] = useState<Promotion[] | null>(null)
  const [promotionId, setPromotionId] = useState<number | ''>('')
  const [etudiants, setEtudiants] = useState<Utilisateur[] | null>(null)
  const [erreur, setErreur] = useState<ErreurApi | null>(null)

  useEffect(() => {
    api
      .promotions()
      .then((liste) => {
        setPromotions(liste)
        if (liste.length > 0) setPromotionId(liste[0].id)
      })
      .catch((e: ErreurApi) => setErreur(e))
  }, [])

  useEffect(() => {
    if (promotionId === '') return
    setEtudiants(null)
    api
      .utilisateurs(promotionId, 'ETUDIANT')
      .then((liste) => {
        setEtudiants(liste)
        onChange(liste.length > 0 ? liste[0].id : '')
      })
      .catch((e: ErreurApi) => setErreur(e))
    // onChange est volontairement hors dépendances : le parent le recrée à
    // chaque rendu, l'inclure relancerait l'appel en boucle.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [promotionId])

  if (erreur) return <Erreur message={erreur.message} code={erreur.code} />
  if (!promotions) return <Chargement quoi="des promotions" />

  return (
    <fieldset className="carte">
      <legend>Qui es-tu ?</legend>

      <div className="champ">
        <label htmlFor="promotion-etudiant">Promotion</label>
        <select
          id="promotion-etudiant"
          value={promotionId}
          onChange={(e) => setPromotionId(Number(e.target.value))}
        >
          {promotions.map((p) => (
            <option key={p.id} value={p.id}>
              {p.nom}
            </option>
          ))}
        </select>
      </div>

      <div className="champ">
        <label htmlFor="etudiant">Ton nom</label>
        {etudiants === null ? (
          <Chargement quoi="de la liste" />
        ) : (
          <select id="etudiant" value={etudiantId} onChange={(e) => onChange(Number(e.target.value))}>
            {etudiants.map((e) => (
              <option key={e.id} value={e.id}>
                {e.nom}
              </option>
            ))}
          </select>
        )}
      </div>
    </fieldset>
  )
}
