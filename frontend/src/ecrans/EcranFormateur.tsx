import { useEffect, useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { Promotion, SessionCreee } from '../api/types'
import { Chargement, Erreur } from '../composants/Etat'

/**
 * Écran formateur — EF1 : ouvrir une session et afficher son code.
 *
 * Le tableau de bord (EF7) et la clôture (EF6) viendront s'ajouter ici,
 * sur leurs propres branches.
 */
export function EcranFormateur() {
  const [promotions, setPromotions] = useState<Promotion[] | null>(null)
  const [promotionId, setPromotionId] = useState<number | ''>('')
  const [titre, setTitre] = useState('')
  const [session, setSession] = useState<SessionCreee | null>(null)
  const [erreur, setErreur] = useState<ErreurApi | null>(null)
  const [envoiEnCours, setEnvoiEnCours] = useState(false)

  useEffect(() => {
    api
      .promotions()
      .then((liste) => {
        setPromotions(liste)
        if (liste.length > 0) setPromotionId(liste[0].id)
      })
      .catch((e: ErreurApi) => setErreur(e))
  }, [])

  async function ouvrir(evenement: React.FormEvent) {
    evenement.preventDefault()
    if (promotionId === '') return
    setEnvoiEnCours(true)
    setErreur(null)
    try {
      setSession(await api.ouvrirSession(titre, promotionId))
    } catch (e) {
      setErreur(e as ErreurApi)
      setSession(null)
    } finally {
      setEnvoiEnCours(false)
    }
  }

  if (!promotions && !erreur) return <Chargement quoi="des promotions" />

  return (
    <section>
      <h2>Ouvrir une session</h2>

      <form onSubmit={ouvrir}>
        <p>
          <label htmlFor="titre">Titre de la séance</label>
          <br />
          <input
            id="titre"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            placeholder="Spring Boot — jour 4"
          />
        </p>

        <p>
          <label htmlFor="promotion">Promotion</label>
          <br />
          <select
            id="promotion"
            value={promotionId}
            onChange={(e) => setPromotionId(Number(e.target.value))}
          >
            {(promotions ?? []).map((p) => (
              <option key={p.id} value={p.id}>
                {p.nom}
              </option>
            ))}
          </select>
        </p>

        <button type="submit" disabled={envoiEnCours || promotionId === ''}>
          {envoiEnCours ? 'Ouverture…' : 'Ouvrir la session'}
        </button>
      </form>

      {erreur && <Erreur message={erreur.message} code={erreur.code} />}

      {session && (
        <div>
          <h3>Code de présence</h3>
          <p style={{ fontSize: '2.5rem', letterSpacing: '0.3rem', fontFamily: 'monospace' }}>
            {session.code}
          </p>
          {/* L'expiration vient de l'API (RG2) : elle n'est pas recalculée ici. */}
          <p>
            Valable jusqu'à{' '}
            <strong>{new Date(session.expirationAt).toLocaleTimeString('fr-FR')}</strong>.
          </p>
        </div>
      )}
    </section>
  )
}
