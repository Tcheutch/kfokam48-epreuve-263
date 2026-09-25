import { useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { ExerciceDepose, Presence } from '../api/types'
import { Erreur } from '../composants/Etat'
import { SelecteurEtudiant } from '../composants/SelecteurEtudiant'

/** Écran étudiant — EF2 (marquer sa présence) et EF3 (déposer son exercice). */
export function EcranEtudiant() {
  const [etudiantId, setEtudiantId] = useState<number | ''>('')
  const [code, setCode] = useState('')
  const [presence, setPresence] = useState<Presence | null>(null)
  const [erreurPresence, setErreurPresence] = useState<ErreurApi | null>(null)
  const [marquageEnCours, setMarquageEnCours] = useState(false)

  const [lien, setLien] = useState('')
  const [exercice, setExercice] = useState<ExerciceDepose | null>(null)
  const [erreurDepot, setErreurDepot] = useState<ErreurApi | null>(null)
  const [depotEnCours, setDepotEnCours] = useState(false)

  async function marquer(evenement: React.FormEvent) {
    evenement.preventDefault()
    if (etudiantId === '') return
    setMarquageEnCours(true)
    setErreurPresence(null)
    setPresence(null)
    try {
      setPresence(await api.marquerPresence(code.trim(), etudiantId))
    } catch (e) {
      setErreurPresence(e as ErreurApi)
    } finally {
      setMarquageEnCours(false)
    }
  }

  async function deposer(evenement: React.FormEvent) {
    evenement.preventDefault()
    if (etudiantId === '' || !presence) return
    setDepotEnCours(true)
    setErreurDepot(null)
    try {
      const depose = await api.deposerExercice(presence.sessionId, etudiantId, lien.trim())
      setExercice(depose)
    } catch (e) {
      setErreurDepot(e as ErreurApi)
      setExercice(null)
    } finally {
      setDepotEnCours(false)
    }
  }

  async function remplacer(evenement: React.FormEvent) {
    evenement.preventDefault()
    if (!exercice) return
    setDepotEnCours(true)
    setErreurDepot(null)
    try {
      const misAJour = await api.remplacerLien(exercice.id, lien.trim())
      setExercice({ id: misAJour.id, statut: misAJour.statut })
    } catch (e) {
      setErreurDepot(e as ErreurApi)
    } finally {
      setDepotEnCours(false)
    }
  }

  return (
    <section>
      <h2>Marquer ma présence</h2>

      <SelecteurEtudiant etudiantId={etudiantId} onChange={setEtudiantId} />

      <form onSubmit={marquer}>
        <p>
          <label htmlFor="code">Code de présence</label>
          <br />
          <input
            id="code"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            maxLength={6}
            placeholder="K7M2QX"
            autoCapitalize="characters"
            style={{
              fontFamily: 'monospace',
              fontSize: '1.5rem',
              letterSpacing: '0.2rem',
              width: '100%',
              boxSizing: 'border-box',
            }}
          />
        </p>

        <button type="submit" disabled={marquageEnCours || etudiantId === '' || code.trim().length === 0}>
          {marquageEnCours ? 'Envoi…' : 'Je suis présent'}
        </button>
      </form>

      {/* Le message affiché est celui de l'API : le front ne sait pas pourquoi
          un code est refusé, et n'a pas à le savoir. */}
      {erreurPresence && <Erreur message={erreurPresence.message} code={erreurPresence.code} />}

      {presence && (
        <>
          <p role="status">
            <strong>Présence enregistrée.</strong>
            {presence.source === 'FORMATEUR' ? ' (ajoutée par le formateur)' : ''}
          </p>

          <h2>Déposer mon exercice</h2>
          <p>
            <small>
              Possible jusqu'à ce que le formateur clôture la séance, même après l'expiration du
              code (RG12).
            </small>
          </p>

          <form onSubmit={exercice ? remplacer : deposer}>
            <p>
              <label htmlFor="lien">Lien de mon travail</label>
              <br />
              <input
                id="lien"
                value={lien}
                onChange={(e) => setLien(e.target.value)}
                placeholder="https://github.com/mon-compte/tp4"
                style={{ width: '100%', boxSizing: 'border-box' }}
              />
            </p>

            <button type="submit" disabled={depotEnCours || lien.trim().length === 0}>
              {depotEnCours ? 'Envoi…' : exercice ? 'Remplacer le lien' : 'Déposer'}
            </button>
          </form>

          {erreurDepot && <Erreur message={erreurDepot.message} code={erreurDepot.code} />}

          {exercice && (
            <p role="status">
              Exercice enregistré — statut <strong>{exercice.statut}</strong>.
            </p>
          )}
        </>
      )}
    </section>
  )
}
