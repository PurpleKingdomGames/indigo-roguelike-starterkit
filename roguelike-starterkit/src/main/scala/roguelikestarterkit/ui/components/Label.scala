package roguelikestarterkit.ui.components

import indigo.*
import indigo.syntax.*
import roguelikestarterkit.terminal.RogueTerminalEmulator
import roguelikestarterkit.terminal.TerminalMaterial
import roguelikestarterkit.tiles.RoguelikeTiles10x10
import roguelikestarterkit.tiles.RoguelikeTiles5x6
import roguelikestarterkit.ui.component.ComponentFragment
import roguelikestarterkit.ui.component.StatelessComponent
import roguelikestarterkit.ui.datatypes.Bounds
import roguelikestarterkit.ui.datatypes.CharSheet
import roguelikestarterkit.ui.datatypes.Coords
import roguelikestarterkit.ui.datatypes.Dimensions
import roguelikestarterkit.ui.datatypes.UiContext

import scala.annotation.targetName

/** Labels are a simple `Component` that render text.
  */
final case class Label[ReferenceData](
    text: ReferenceData => String,
    bounds: Bounds,
    render: (Coords, String, Dimensions) => Outcome[ComponentFragment]
):
  def withText(value: String): Label[ReferenceData] =
    this.copy(text = _ => value)
  def withText(f: ReferenceData => String): Label[ReferenceData] =
    this.copy(text = f)

  def withBounds(value: Bounds): Label[ReferenceData] =
    this.copy(bounds = value)

  // Delegates, for convenience.

  def update[StartupData, ContextData](
      context: UiContext[ReferenceData]
  ): GlobalEvent => Outcome[Label[ReferenceData]] =
    summon[StatelessComponent[Label[ReferenceData], ReferenceData]].updateModel(context, this)

  def present[StartupData, ContextData](
      context: UiContext[ReferenceData]
  ): Outcome[ComponentFragment] =
    summon[StatelessComponent[Label[ReferenceData], ReferenceData]].present(context, this)

  def reflow: Label[ReferenceData] =
    summon[StatelessComponent[Label[ReferenceData], ReferenceData]].reflow(this)

  def cascade(parentBounds: Bounds): Label[ReferenceData] =
    summon[StatelessComponent[Label[ReferenceData], ReferenceData]].cascade(this, parentBounds)

object Label:

  private def findBounds(text: String): Bounds =
    Bounds(0, 0, text.length, 1)

  /** Minimal label constructor with custom rendering function
    */
  def apply[ReferenceData](text: String)(
      present: (Coords, String, Dimensions) => Outcome[ComponentFragment]
  ): Label[ReferenceData] =
    Label(_ => text, findBounds(text), present)

  @targetName("Label_apply_curried")
  def apply[ReferenceData](text: ReferenceData => String)(
      present: (Coords, String, Dimensions) => Outcome[ComponentFragment]
  ): Label[ReferenceData] =
    Label(text, Bounds(0, 0, 1, 1), present)

  private val graphic = Graphic(0, 0, TerminalMaterial(AssetName(""), RGBA.White, RGBA.Black))

  private def presentLabel(
      charSheet: CharSheet,
      fgColor: RGBA,
      bgColor: RGBA
  ): (Coords, String, Dimensions) => Outcome[ComponentFragment] = {
    case (offset, label, dimensions) =>
      val size = dimensions.unsafeToSize

      val terminal =
        RogueTerminalEmulator(size)
          .putLine(Point.zero, label, fgColor, bgColor)
          .toCloneTiles(
            CloneId(s"label_${charSheet.assetName.toString}"),
            offset.toScreenSpace(charSheet.size),
            charSheet.charCrops
          ) { case (fg, bg) =>
            graphic.withMaterial(TerminalMaterial(charSheet.assetName, fg, bg))
          }

      Outcome(ComponentFragment(terminal))
  }

  /** Creates a Label rendered using the RogueTerminalEmulator based on a `Label.Theme`, with bounds
    * based on the text length
    */
  def apply[ReferenceData](text: String, theme: Theme): Label[ReferenceData] =
    Label(
      _ => text,
      findBounds(text),
      presentLabel(theme.charSheet, theme.colors.foreground, theme.colors.background)
    )

  /** Creates a Label rendered using the RogueTerminalEmulator based on a `Label.Theme`, with custom
    * bounds
    */
  def apply[ReferenceData](text: ReferenceData => String, theme: Theme): Label[ReferenceData] =
    Label(
      text,
      Bounds(0, 0, 1, 1),
      presentLabel(theme.charSheet, theme.colors.foreground, theme.colors.background)
    )

  given [ReferenceData]: StatelessComponent[Label[ReferenceData], ReferenceData] with
    def bounds(model: Label[ReferenceData]): Bounds =
      model.bounds

    def updateModel(
        context: UiContext[ReferenceData],
        model: Label[ReferenceData]
    ): GlobalEvent => Outcome[Label[ReferenceData]] =
      case FrameTick =>
        Outcome(model.withBounds(findBounds(model.text(context.reference))))

      case _ =>
        Outcome(model)

    def present(
        context: UiContext[ReferenceData],
        model: Label[ReferenceData]
    ): Outcome[ComponentFragment] =
      model.render(context.bounds.coords, model.text(context.reference), model.bounds.dimensions)

  final case class Theme(
      charSheet: CharSheet,
      colors: TerminalTileColors
  ):
    def withCharSheet(value: CharSheet): Theme =
      this.copy(charSheet = value)

    def withColors(foreground: RGBA, background: RGBA): Theme =
      this.copy(colors = TerminalTileColors(foreground, background))

  object Theme:
    def apply(charSheet: CharSheet, foreground: RGBA, background: RGBA): Theme =
      Theme(
        charSheet,
        TerminalTileColors(foreground, background)
      )

    def apply(charSheet: CharSheet): Theme =
      Theme(charSheet, RGBA.White, RGBA.Black)
