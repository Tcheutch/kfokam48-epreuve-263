import type {
  Exercice,
  ExerciceDepose,
  Presence,
  Promotion,
  RelectureAFaire,
  Session,
  SessionCreee,
  Utilisateur,
} from './types'

/**
 * Couche d'accès à l'API — le SEUL endroit du frontend qui connaisse `fetch`
 * et les adresses du backend (contrainte F3).
 *
 * Aucune règle de gestion ici : ce module transporte, il ne décide pas. Les
 * moyennes, les expirations et les statuts viennent de l'API.
 */

/** Une erreur renvoyée par l'API, au format imposé { code, message }. */
export class ErreurApi extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly statut: number,
  ) {
    super(message)
    this.name = 'ErreurApi'
  }
}

const MESSAGE_RESEAU = "Le serveur est injoignable. Vérifiez qu'il est démarré."

async function appeler<T>(chemin: string, options: RequestInit = {}): Promise<T> {
  let reponse: Response
  try {
    reponse = await fetch(`/api${chemin}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    throw new ErreurApi('RESEAU_INDISPONIBLE', MESSAGE_RESEAU, 0)
  }

  if (!reponse.ok) {
    // Le contrat garantit { code, message } sur TOUTES les erreurs. On se garde
    // malgré tout d'un corps illisible : le front ne doit jamais afficher
    // « undefined » à un utilisateur.
    const corps = await reponse.json().catch(() => null)
    throw new ErreurApi(
      corps?.code ?? 'ERREUR_INCONNUE',
      corps?.message ?? `Erreur ${reponse.status}.`,
      reponse.status,
    )
  }

  if (reponse.status === 204) {
    return undefined as T
  }
  return (await reponse.json()) as T
}

export const api = {
  promotions: () => appeler<Promotion[]>('/promotions'),

  utilisateurs: (promotionId: number, role?: 'FORMATEUR' | 'ETUDIANT') =>
    appeler<Utilisateur[]>(
      `/promotions/${promotionId}/utilisateurs${role ? `?role=${role}` : ''}`,
    ),

  /** EF1 — le formateur ouvre une session et obtient son code. */
  ouvrirSession: (titre: string, promotionId: number) =>
    appeler<SessionCreee>('/sessions', {
      method: 'POST',
      body: JSON.stringify({ titre, promotionId }),
    }),

  /** EF12 — les séances d'une promotion et leur état. */
  sessions: (promotionId: number) => appeler<Session[]>(`/sessions?promotionId=${promotionId}`),

  /** EF6 — le formateur clôture une séance. Irréversible (RG20). */
  cloturerSession: (sessionId: number) =>
    appeler<Session>(`/sessions/${sessionId}/cloture`, { method: 'POST' }),

  /** EF2 — l'étudiant marque sa présence. */
  marquerPresence: (code: string, etudiantId: number) =>
    appeler<Presence>('/presences', {
      method: 'POST',
      body: JSON.stringify({ code, etudiantId }),
    }),

  /** EF3 — l'étudiant dépose le lien de son exercice. */
  deposerExercice: (sessionId: number, etudiantId: number, lien: string) =>
    appeler<ExerciceDepose>('/exercices', {
      method: 'POST',
      body: JSON.stringify({ sessionId, etudiantId, lien }),
    }),

  /** RG13 — remplacer le lien, tant que la relecture n'est pas rendue. */
  remplacerLien: (exerciceId: number, lien: string) =>
    appeler<Exercice>(`/exercices/${exerciceId}`, {
      method: 'PUT',
      body: JSON.stringify({ lien }),
    }),

  /** RG11 — les relectures assignées à un étudiant. */
  mesRelectures: (etudiantId: number) =>
    appeler<RelectureAFaire[]>(`/etudiants/${etudiantId}/relectures`),

  /** EF5 — rendre une note et un commentaire. */
  rendreRelecture: (relectureId: number, note: number, commentaire: string, relecteurId: number) =>
    appeler<unknown>(`/relectures/${relectureId}`, {
      method: 'POST',
      body: JSON.stringify({ note, commentaire, relecteurId }),
    }),
}
