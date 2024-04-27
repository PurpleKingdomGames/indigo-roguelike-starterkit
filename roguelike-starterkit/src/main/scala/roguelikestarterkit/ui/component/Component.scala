package roguelikestarterkit.ui.component

import indigo.*
import roguelikestarterkit.ui.datatypes.Bounds
import roguelikestarterkit.ui.datatypes.UiContext

/** A typeclass that confirms that some type `A` can be used as a `Component` provides the necessary
  * operations for that type to act as a component.
  */
trait Component[A]:

  /** The position and size of the component
    */
  def bounds(model: A): Bounds

  /** Update this componenets model.
    */
  def updateModel[StartupData, ContextData](
      context: UiContext[StartupData, ContextData],
      model: A
  ): GlobalEvent => Outcome[A]

  /** Produce a renderable output for this component, based on the component's model.
    */
  def present[StartupData, ContextData](
      context: UiContext[StartupData, ContextData],
      model: A
  ): Outcome[ComponentFragment]

  /** Used internally to instruct the component that the layout has changed in some way, and that it
    * should reflow it's contents - whatever that means in the context of this component type.
    */
  def reflow(model: A): A
