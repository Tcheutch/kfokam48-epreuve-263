/**
 * Les deux états que chaque écran doit gérer explicitement (contrainte F3) :
 * le chargement et l'erreur. Regroupés ici pour qu'aucun écran ne les oublie.
 *
 * Ils ont une forme, désormais : un bloc identifiable plutôt qu'une ligne de
 * texte perdue. L'erreur reste lisible sans être agressive — un message qu'on
 * n'ose pas lire est un message qui ne sert à rien.
 */

export function Chargement({ quoi }: { quoi: string }) {
  return (
    <p className="etat-bloc etat-bloc--chargement" role="status">
      Chargement {quoi}…
    </p>
  )
}

export function Erreur({ message, code }: { message: string; code?: string }) {
  return (
    <div className="etat-bloc etat-bloc--erreur" role="alert">
      <span className="etat-bloc__message">{message}</span>
      {code ? <span className="etat-bloc__code">{code}</span> : null}
    </div>
  )
}
