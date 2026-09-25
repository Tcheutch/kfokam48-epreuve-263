import { useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { Presence } from '../api/types'
import { Erreur } from '../composants/Etat'
import { SelecteurEtudiant } from '../composants/SelecteurEtudiant'

/**
 * Écran étudiant — EF2 : marquer sa présence avec le code.
 *
 * Le dépôt d'exercice (EF3) viendra s'ajouter ici, sur sa propre branche.
 */
export function EcranEtudiant() {
  const [etudiantId, setEtudiantId] = useState<number | ''>('')
  const [code, setCode] = useState('')
  const [presence, setPresence] = useState<Presence | null>(null)
  const [erreur, setErreur] = useState<ErreurApi | null>(null)
  const [envoiEnCours, setEnvoiEnCours] = useState(false)

  async function marquer(evenement: React.FormEvent) {
    evenement.preventDefault()
    if (etudiantId === '') return
    setEnvoiEnCours(true)
    setErreur(null)
    setPresence(null)
    try {
      setPresence(await api.marquerPresence(code.trim(), etudiantId))
    } catch (e) {
      setErreur(e as ErreurApi)
    } finally {
      setEnvoiEnCours(false)
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
            inputMode="text"
            autoCapitalize="characters"
            style={{ fontFamily: 'monospace', fontSize: '1.5rem', letterSpacing: '0.2rem', width: '100%' }}
          />
        </p>

        <button type="submit" disabled={envoiEnCours || etudiantId === '' || code.trim().length === 0}>
          {envoiEnCours ? 'Envoi…' : 'Je suis présent'}
        </button>
      </form>

      {/* Le message affiché est celui que l'API renvoie : le front ne réécrit
          pas les règles, il ne sait pas pourquoi un code est refusé. */}
      {erreur && <Erreur message={erreur.message} code={erreur.code} />}

      {presence && (
        <p role="status">
          <strong>Présence enregistrée.</strong>
          {presence.source === 'FORMATEUR' ? ' (ajoutée par le formateur)' : ''}
        </p>
      )}
    </section>
  )
}
