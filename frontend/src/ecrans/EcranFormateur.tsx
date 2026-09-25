import { useCallback, useEffect, useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { Promotion, Session, SessionCreee } from '../api/types'
import { Chargement, Erreur } from '../composants/Etat'
import { TableauPromotion } from '../composants/TableauPromotion'

/**
 * Écran formateur — EF1 (ouvrir une séance), EF12 (les lister avec leur état)
 * et EF6 (les clôturer).
 *
 * Les trois états affichés — ouverte, expirée, clôturée — viennent de l'API
 * (champ `etat`). Le front ne les recalcule pas à partir des dates : ce serait
 * dupliquer RG2 et RG20.
 */
export function EcranFormateur() {
  const [promotions, setPromotions] = useState<Promotion[] | null>(null)
  const [promotionId, setPromotionId] = useState<number | ''>('')
  const [titre, setTitre] = useState('')
  const [nouvelleSession, setNouvelleSession] = useState<SessionCreee | null>(null)
  const [sessions, setSessions] = useState<Session[] | null>(null)
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

  const rechargerSessions = useCallback(async () => {
    if (promotionId === '') return
    try {
      setSessions(await api.sessions(promotionId))
    } catch (e) {
      setErreur(e as ErreurApi)
    }
  }, [promotionId])

  useEffect(() => {
    void rechargerSessions()
  }, [rechargerSessions])

  async function ouvrir(evenement: React.FormEvent) {
    evenement.preventDefault()
    if (promotionId === '') return
    setEnvoiEnCours(true)
    setErreur(null)
    try {
      setNouvelleSession(await api.ouvrirSession(titre, promotionId))
      setTitre('')
      await rechargerSessions()
    } catch (e) {
      setErreur(e as ErreurApi)
      setNouvelleSession(null)
    } finally {
      setEnvoiEnCours(false)
    }
  }

  async function cloturer(session: Session) {
    // RG20 — irréversible : on le dit avant, pas après.
    if (!window.confirm(`Clôturer « ${session.titre} » ? C'est définitif : plus aucun dépôt, plus aucune note.`)) {
      return
    }
    setErreur(null)
    try {
      await api.cloturerSession(session.id)
      if (nouvelleSession?.id === session.id) setNouvelleSession(null)
      await rechargerSessions()
    } catch (e) {
      setErreur(e as ErreurApi)
    }
  }

  if (!promotions && !erreur) return <Chargement quoi="des promotions" />

  return (
    <section>
      <h2>Ouvrir une séance</h2>

      <form onSubmit={ouvrir}>
        <p>
          <label htmlFor="titre">Titre de la séance</label>
          <br />
          <input
            id="titre"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            placeholder="Spring Boot — jour 4"
            style={{ width: '100%', boxSizing: 'border-box' }}
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
          {envoiEnCours ? 'Ouverture…' : 'Ouvrir la séance'}
        </button>
      </form>

      {erreur && <Erreur message={erreur.message} code={erreur.code} />}

      {nouvelleSession && (
        <div>
          <h3>Code de présence</h3>
          <p style={{ fontSize: '2.5rem', letterSpacing: '0.3rem', fontFamily: 'monospace' }}>
            {nouvelleSession.code}
          </p>
          {/* L'heure d'expiration vient de l'API (RG2), elle n'est pas recalculée ici. */}
          <p>
            Valable jusqu'à{' '}
            <strong>{new Date(nouvelleSession.expirationAt).toLocaleTimeString('fr-FR')}</strong>.
          </p>
        </div>
      )}

      <h2>Suivi de la promotion</h2>
      {promotionId !== '' && <TableauPromotion promotionId={promotionId} key={`${promotionId}-${sessions?.length}`} />}

      <h2>Mes séances</h2>
      {sessions === null ? (
        <Chargement quoi="des séances" />
      ) : sessions.length === 0 ? (
        <p>Aucune séance pour cette promotion.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th align="left">Séance</th>
              <th align="left">Code</th>
              <th align="left">État</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {sessions.map((session) => (
              <tr key={session.id}>
                <td>{session.titre}</td>
                {/* Le code n'a de sens que tant qu'il marche. */}
                <td style={{ fontFamily: 'monospace' }}>
                  {session.etat === 'OUVERTE' ? session.code : '—'}
                </td>
                <td>{libelleEtat(session)}</td>
                <td>
                  {session.etat !== 'CLOTUREE' && (
                    <button onClick={() => cloturer(session)}>Clôturer</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}

/** Les trois notions restent distinctes jusque dans les mots affichés (H1). */
function libelleEtat(session: Session): string {
  switch (session.etat) {
    case 'OUVERTE':
      return 'Ouverte — le code marche'
    case 'EXPIREE':
      return 'Code expiré — les dépôts restent possibles'
    case 'CLOTUREE':
      return 'Clôturée — tout est figé'
  }
}
