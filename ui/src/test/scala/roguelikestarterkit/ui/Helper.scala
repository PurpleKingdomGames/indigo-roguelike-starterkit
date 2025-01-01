package roguelikestarterkit.ui

import indigo.shared.Outcome
import indigo.shared.events.GlobalEvent
import indigo.shared.scenegraph.Layer
import roguelikestarterkit.ui.component.Component
import roguelikestarterkit.ui.datatypes.Dimensions
import roguelikestarterkit.ui.datatypes.UIContext

object Helper:

  extension [A, ReferenceData](component: A)(using c: Component[A, ReferenceData])
    def update[StartupData, ContextData](
        context: UIContext[ReferenceData]
    ): GlobalEvent => Outcome[A] =
      c.updateModel(context, component)

    def present[StartupData, ContextData](
        context: UIContext[ReferenceData]
    ): Outcome[Layer] =
      c.present(context, component)

    def refresh(
        reference: ReferenceData,
        parentDimensions: Dimensions
    ): A =
      c.refresh(reference, component, parentDimensions)

