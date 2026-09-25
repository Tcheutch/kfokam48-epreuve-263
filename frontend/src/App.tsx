import { useState } from 'react'
import { EcranFormateur } from './ecrans/EcranFormateur'

/**
 * Trois écrans, un par rôle (contrainte F2). Sans authentification (Q1, RG1),
 * le rôle se choisit simplement ici.
 */
type Onglet = 'formateur' | 'etudiant' | 'relecteur'

export default function App() {
  const [onglet, setOnglet] = useState<Onglet>('formateur')

  return (
    <main style={{ fontFamily: 'system-ui, sans-serif', maxWidth: 720, margin: '2rem auto', padding: '0 1rem' }}>
      <h1>KFOKAM48 — Présence &amp; Relecture</h1>

      <nav style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
        <button onClick={() => setOnglet('formateur')} disabled={onglet === 'formateur'}>
          Formateur
        </button>
        <button onClick={() => setOnglet('etudiant')} disabled={onglet === 'etudiant'}>
          Étudiant
        </button>
        <button onClick={() => setOnglet('relecteur')} disabled={onglet === 'relecteur'}>
          Relecteur
        </button>
      </nav>

      {onglet === 'formateur' && <EcranFormateur />}
      {onglet === 'etudiant' && <p>Écran étudiant — à venir (EF2, EF3).</p>}
      {onglet === 'relecteur' && <p>Écran relecteur — à venir (EF5).</p>}
    </main>
  )
}
