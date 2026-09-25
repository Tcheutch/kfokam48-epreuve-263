import { useCallback, useEffect, useState } from 'react'
import { api, ErreurApi } from '../api/client'
import type { RelectureAFaire } from '../api/types'
import { Chargement, Erreur } from '../composants/Etat'
import { SelecteurEtudiant } from '../composants/SelecteurEtudiant'

/** Écran relecteur — EF5 : rendre une note et un commentaire. */
export function EcranRelecteur() {
  const [etudiantId, setEtudiantId] = useState<number | ''>('')
  const [relectures, setRelectures] = useState<RelectureAFaire[] | null>(null)
  const [erreur, setErreur] = useState<ErreurApi | null>(null)

  const recharger = useCallback(async () => {
    if (etudiantId === '') return
    setRelectures(null)
    setErreur(null)
    try {
      setRelectures(await api.mesRelectures(etudiantId))
    } catch (e) {
      setErreur(e as ErreurApi)
    }
  }, [etudiantId])

  useEffect(() => {
    void recharger()
  }, [recharger])

  return (
    <section>
      <h2>Mes relectures</h2>

      <SelecteurEtudiant etudiantId={etudiantId} onChange={setEtudiantId} />

      {erreur && <Erreur message={erreur.message} code={erreur.code} />}
      {etudiantId !== '' && relectures === null && !erreur && <Chargement quoi="des relectures" />}

      {relectures?.length === 0 && <p>Aucune relecture ne t'est assignée pour le moment.</p>}

      {relectures?.map((relecture) => (
        <FormulaireRelecture
          key={relecture.id}
          relecture={relecture}
          relecteurId={etudiantId as number}
          onRendue={recharger}
        />
      ))}
    </section>
  )
}

function FormulaireRelecture({
  relecture,
  relecteurId,
  onRendue,
}: {
  relecture: RelectureAFaire
  relecteurId: number
  onRendue: () => void
}) {
  const [note, setNote] = useState(relecture.note?.toString() ?? '')
  const [commentaire, setCommentaire] = useState(relecture.commentaire ?? '')
  const [erreur, setErreur] = useState<ErreurApi | null>(null)
  const [envoiEnCours, setEnvoiEnCours] = useState(false)

  async function envoyer(evenement: React.FormEvent) {
    evenement.preventDefault()
    setEnvoiEnCours(true)
    setErreur(null)
    try {
      // La note part telle que saisie : c'est l'API qui décide si elle est
      // valide (RG9). Le front ne réimplémente pas la règle, il ne fait
      // qu'éviter d'envoyer un champ vide.
      await api.rendreRelecture(relecture.id, Number(note), commentaire, relecteurId)
      onRendue()
    } catch (e) {
      setErreur(e as ErreurApi)
    } finally {
      setEnvoiEnCours(false)
    }
  }

  return (
    <article style={{ border: '1px solid #ccc', padding: '1rem', marginBottom: '1rem' }}>
      <h3>
        {relecture.sessionTitre} — travail de {relecture.auteurNom}
      </h3>

      <p>
        <a href={relecture.lien} target="_blank" rel="noreferrer">
          {relecture.lien}
        </a>
      </p>

      {!relecture.modifiable ? (
        <p>
          Séance clôturée — note figée à <strong>{relecture.note ?? '—'}</strong> (RG20).
        </p>
      ) : (
        <form onSubmit={envoyer}>
          <p>
            <label htmlFor={`note-${relecture.id}`}>Note sur 20</label>
            <br />
            <input
              id={`note-${relecture.id}`}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              inputMode="numeric"
            />
          </p>
          <p>
            <label htmlFor={`commentaire-${relecture.id}`}>Commentaire</label>
            <br />
            <textarea
              id={`commentaire-${relecture.id}`}
              value={commentaire}
              onChange={(e) => setCommentaire(e.target.value)}
              rows={3}
              style={{ width: '100%', boxSizing: 'border-box' }}
            />
          </p>
          <button type="submit" disabled={envoiEnCours || note.trim().length === 0}>
            {envoiEnCours ? 'Envoi…' : relecture.statut === 'RENDUE' ? 'Corriger ma note' : 'Rendre ma relecture'}
          </button>
          {relecture.statut === 'RENDUE' && (
            <p>
              <small>Déjà rendue. Corrigeable tant que le formateur n'a pas clôturé (Q10).</small>
            </p>
          )}
        </form>
      )}

      {erreur && <Erreur message={erreur.message} code={erreur.code} />}
    </article>
  )
}
