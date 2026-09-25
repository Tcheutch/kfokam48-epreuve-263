/**
 * Les deux états que chaque écran doit gérer explicitement (contrainte F3) :
 * le chargement et l'erreur. Regroupés ici pour qu'aucun écran ne les oublie.
 */

export function Chargement({ quoi }: { quoi: string }) {
  return <p role="status">Chargement {quoi}…</p>
}

export function Erreur({ message, code }: { message: string; code?: string }) {
  return (
    <p role="alert" style={{ color: '#b00020' }}>
      {message}
      {code ? <small> ({code})</small> : null}
    </p>
  )
}
