import { useCallback, useEffect, useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { EtatSession, Promotion, Session, SessionCreee } from '../api/types'
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

      <form className="carte" onSubmit={ouvrir}>
        <div className="champ">
          <label htmlFor="titre">Titre de la séance</label>
          <input
            id="titre"
            value={titre}
            onChange={(e) => setTitre(e.target.value)}
            placeholder="Spring Boot — jour 4"
          />
        </div>

        <div className="champ">
          <label htmlFor="promotion">Promotion</label>
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
        </div>

        <button className="bouton-principal" type="submit" disabled={envoiEnCours || promotionId === ''}>
          {envoiEnCours ? 'Ouverture…' : 'Ouvrir la séance'}
        </button>
      </form>

      {erreur && <Erreur message={erreur.message} code={erreur.code} />}

      {nouvelleSession && <CodeDePresence session={nouvelleSession} />}

      <h2>Suivi de la promotion</h2>
      {promotionId !== '' && (
        <TableauPromotion promotionId={promotionId} key={`${promotionId}-${sessions?.length}`} />
      )}

      <h2>Mes séances</h2>
      {sessions === null ? (
        <Chargement quoi="des séances" />
      ) : sessions.length === 0 ? (
        <p className="vide">Aucune séance pour cette promotion.</p>
      ) : (
        <div className="tableau-enveloppe">
          <table className="tableau">
            <thead>
              <tr>
                <th scope="col">Séance</th>
                <th scope="col">Code</th>
                <th scope="col">État</th>
                <th scope="col">Action</th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((session) => (
                <tr key={session.id}>
                  <td data-role="titre">{session.titre}</td>
                  <td data-label="Code">
                    {/* Le code n'a de sens que tant qu'il marche. */}
                    {session.etat === 'OUVERTE' ? (
                      <span className="code-seance">{session.code}</span>
                    ) : (
                      <span className="valeur-vide">—</span>
                    )}
                  </td>
                  <td data-label="État">
                    <span className={`pastille pastille--${classeEtat(session.etat)}`}>
                      {session.etat}
                    </span>
                    <span className="etat-detail">{libelleEtat(session)}</span>
                  </td>
                  <td data-label="Action" data-role="action">
                    {session.etat !== 'CLOTUREE' && (
                      <button
                        type="button"
                        className="bouton-irreversible"
                        onClick={() => cloturer(session)}
                      >
                        Clôturer définitivement
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

/**
 * Le code de présence est l'information la plus importante de cet écran : il
 * est lu à voix haute dans une salle, parfois depuis le fond. Il mérite sa
 * place, et un bouton pour le copier quand le formateur le diffuse autrement.
 */
function CodeDePresence({ session }: { session: SessionCreee }) {
  const [copie, setCopie] = useState(false)

  async function copier() {
    try {
      await navigator.clipboard.writeText(session.code)
      setCopie(true)
      window.setTimeout(() => setCopie(false), 2000)
    } catch {
      // Le presse-papiers peut être refusé par le navigateur : le code reste
      // affiché en grand, donc rien n'est perdu. On ne montre pas d'erreur
      // pour une commodité.
    }
  }

  return (
    <div className="bloc-code">
      <p className="bloc-code__intitule">Code de présence</p>
      <p className="bloc-code__valeur">{session.code}</p>
      {/* L'expiration vient de l'API (RG2) : elle n'est pas recalculée ici. */}
      <p className="bloc-code__validite">
        Valable jusqu'à{' '}
        <strong>{new Date(session.expirationAt).toLocaleTimeString('fr-FR')}</strong>.
      </p>
      <button type="button" className="bouton-discret" onClick={copier}>
        {copie ? 'Copié' : 'Copier le code'}
      </button>
    </div>
  )
}

function classeEtat(etat: EtatSession): string {
  switch (etat) {
    case 'OUVERTE':
      return 'ouverte'
    case 'EXPIREE':
      return 'expiree'
    case 'CLOTUREE':
      return 'cloturee'
  }
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
