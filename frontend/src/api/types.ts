// Les types renvoyés par l'API, tels que décrits dans api/contrat.yaml.
// Ils sont déclarés ici et nulle part ailleurs.

export type Role = 'FORMATEUR' | 'ETUDIANT'
export type EtatSession = 'OUVERTE' | 'EXPIREE' | 'CLOTUREE'
export type SourcePresence = 'ETUDIANT' | 'FORMATEUR'
export type StatutExercice = 'DEPOSE' | 'EN_ATTENTE' | 'RELU'
export type StatutRelecture = 'ASSIGNEE' | 'RENDUE'

export interface Promotion {
  id: number
  nom: string
}

export interface Utilisateur {
  id: number
  nom: string
  role: Role
  promotionId: number | null
}

export interface SessionCreee {
  id: number
  code: string
  ouvertureAt: string
  expirationAt: string
}

export interface Session {
  id: number
  titre: string
  code: string
  ouvertureAt: string
  expirationAt: string
  clotureAt: string | null
  etat: EtatSession
  promotionId: number
}

export interface Presence {
  id: number
  sessionId: number
  etudiantId: number
  nom?: string
  source: SourcePresence
  marqueeAt?: string
}

export interface LigneTableau {
  etudiantId: number
  nom: string
  presences: number
  exercicesDeposes: number
  /** Calculée par l'API, jamais ici : aucune règle de gestion n'est dupliquée (F3). */
  moyenne: number | null
  relecturesEnAttente: number
}

export interface ExerciceDepose {
  id: number
  statut: StatutExercice
}

export interface Exercice {
  id: number
  sessionId: number
  etudiantId: number
  lien: string
  statut: StatutExercice
  deposeAt: string
}
