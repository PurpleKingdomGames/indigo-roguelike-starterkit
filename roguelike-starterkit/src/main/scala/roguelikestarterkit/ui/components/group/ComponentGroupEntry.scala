package roguelikestarterkit.ui.components.group

import indigo.*
import roguelikestarterkit.ui.component.Component
import roguelikestarterkit.ui.datatypes.Bounds
import roguelikestarterkit.ui.datatypes.Coords

/** `ComponentEntry`s record a components model, position, and relevant component typeclass instance
  * for use inside a `ComponentGroup`.
  */
final case class ComponentGroupEntry[A, ReferenceData](
    offset: Coords,
    model: A,
    component: Component[A, ReferenceData]
):

  def cascade(parentBounds: Bounds): ComponentGroupEntry[A, ReferenceData] =
    this.copy(model = component.cascade(model, parentBounds))