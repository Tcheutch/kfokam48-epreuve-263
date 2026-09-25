import { useState } from 'react'
import { EcranEtudiant } from './ecrans/EcranEtudiant'
import { EcranFormateur } from './ecrans/EcranFormateur'
import { EcranRelecteur } from './ecrans/EcranRelecteur'

/**
 * Trois écrans, un par rôle (contrainte F2). Sans authentification (Q1, RG1),
 * le rôle se choisit simplement ici.
 *
 * Les trois boutons portent la sémantique ARIA d'un jeu d'onglets : l'écran
 * actif est annoncé par `aria-selected`, et c'est la même information qui
 * porte son style. Un seul état, deux lectures — on ne peut pas les
 * désynchroniser.
 */
type Onglet = 'formateur' | 'etudiant' | 'relecteur'

const ONGLETS: { cle: Onglet; libelle: string }[] = [
  { cle: 'formateur', libelle: 'Formateur' },
  { cle: 'etudiant', libelle: 'Étudiant' },
  { cle: 'relecteur', libelle: 'Relecteur' },
]

export default function App() {
  const [onglet, setOnglet] = useState<Onglet>('formateur')

  return (
    <main className="page">
      <h1>KFOKAM48 — Présence &amp; Relecture</h1>

      <div className="onglets" role="tablist" aria-label="Choisir son rôle">
        {ONGLETS.map(({ cle, libelle }) => (
          <button
            key={cle}
            type="button"
            role="tab"
            id={`onglet-${cle}`}
            className="onglet"
            aria-selected={onglet === cle}
            aria-controls={`panneau-${cle}`}
            onClick={() => setOnglet(cle)}
          >
            {libelle}
          </button>
        ))}
      </div>

      <div role="tabpanel" id={`panneau-${onglet}`} aria-labelledby={`onglet-${onglet}`}>
        {onglet === 'formateur' && <EcranFormateur />}
        {onglet === 'etudiant' && <EcranEtudiant />}
        {onglet === 'relecteur' && <EcranRelecteur />}
      </div>
    </main>
  )
}
