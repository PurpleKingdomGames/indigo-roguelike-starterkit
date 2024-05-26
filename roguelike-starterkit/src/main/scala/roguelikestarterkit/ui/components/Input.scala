package roguelikestarterkit.ui.components

import indigo.*
import indigo.syntax.*
import roguelikestarterkit.Tile
import roguelikestarterkit.terminal.RogueTerminalEmulator
import roguelikestarterkit.terminal.TerminalMaterial
import roguelikestarterkit.tiles.RoguelikeTiles10x10
import roguelikestarterkit.tiles.RoguelikeTiles5x6
import roguelikestarterkit.ui.component.Component
import roguelikestarterkit.ui.component.ComponentFragment
import roguelikestarterkit.ui.datatypes.Bounds
import roguelikestarterkit.ui.datatypes.CharSheet
import roguelikestarterkit.ui.datatypes.Coords
import roguelikestarterkit.ui.datatypes.Dimensions
import roguelikestarterkit.ui.datatypes.UiContext

import scala.annotation.tailrec
import scala.annotation.targetName

/** Input components allow the user to input text information.
  */
final case class Input(
    text: String,
    bounds: Bounds,
    render: (Coords, Bounds, Input, Seconds) => Outcome[ComponentFragment],
    change: String => Batch[GlobalEvent],
    //
    characterLimit: Int,
    cursor: Cursor,
    hasFocus: Boolean,
    onFocus: () => Batch[GlobalEvent],
    onLoseFocus: () => Batch[GlobalEvent]
):
  lazy val length: Int = text.length

  def withText(value: String): Input =
    this.copy(text = value)

  def withBounds(value: Bounds): Input =
    this.copy(bounds = value)

  def withWidth(value: Int): Input =
    this.copy(bounds = bounds.withDimensions(value, bounds.height))

  def onChange(events: String => Batch[GlobalEvent]): Input =
    this.copy(change = events)
  def onChange(events: Batch[GlobalEvent]): Input =
    onChange(_ => events)
  def onChange(events: GlobalEvent*): Input =
    onChange(Batch.fromSeq(events))

  def noCursorBlink: Input =
    this.copy(cursor = cursor.noCursorBlink)
  def withCursorBlinkRate(interval: Seconds): Input =
    this.copy(cursor = cursor.withCursorBlinkRate(interval))

  def giveFocus: Outcome[Input] =
    Outcome(
      this.copy(hasFocus = true, cursor = cursor.moveCursorTo(length, length)),
      onFocus()
    )

  def loseFocus: Outcome[Input] =
    Outcome(
      this.copy(hasFocus = false),
      onLoseFocus()
    )

  def withCharacterLimit(limit: Int): Input =
    this.copy(characterLimit = limit)

  def withLastCursorMove(value: Seconds): Input =
    this.copy(cursor = cursor.withLastCursorMove(value))

  def cursorLeft: Input =
    this.copy(cursor = cursor.cursorLeft)

  def cursorRight: Input =
    this.copy(cursor = cursor.cursorRight(length))

  def cursorHome: Input =
    this.copy(cursor = cursor.cursorHome)

  def moveCursorTo(newCursorPosition: Int): Input =
    if newCursorPosition >= 0 && newCursorPosition < length then
      this.copy(cursor = cursor.moveTo(newCursorPosition))
    else if newCursorPosition < 0 then this.copy(cursor = cursor.moveTo(0))
    else this.copy(cursor = cursor.moveTo(length - 1))

  def cursorEnd: Input =
    this.copy(cursor = cursor.cursorEnd(length))

  def delete: Input =
    if cursor.cursorPosition == length then this
    else {
      val splitString = text.splitAt(cursor.cursorPosition)
      copy(text = splitString._1 + splitString._2.substring(1))
    }

  def backspace: Input = {
    val splitString = text.splitAt(cursor.cursorPosition)

    this.copy(
      text = splitString._1.take(splitString._1.length - 1) + splitString._2,
      cursor = cursor.moveTo(
        if (cursor.cursorPosition > 0) cursor.cursorPosition - 1 else cursor.cursorPosition
      )
    )
  }

  def addCharacter(char: Char): Input =
    addCharacterText(char.toString())

  def addCharacterText(textToInsert: String): Input = {
    @tailrec
    def rec(remaining: List[Char], textHead: String, textTail: String, position: Int): Input =
      remaining match {
        case Nil =>
          this.copy(
            text = textHead + textTail,
            cursor = cursor.moveTo(position)
          )

        case _ if (textHead + textTail).length >= characterLimit =>
          rec(Nil, textHead, textTail, position)

        case c :: cs if c != '\n' =>
          rec(cs, textHead + c.toString(), textTail, position + 1)

        case _ :: cs =>
          rec(cs, textHead, textTail, position)
      }

    val splitString = text.splitAt(cursor.cursorPosition)

    rec(textToInsert.toCharArray().toList, splitString._1, splitString._2, cursor.cursorPosition)
  }

  def withFocusActions(actions: GlobalEvent*): Input =
    withFocusActions(Batch.fromSeq(actions))
  def withFocusActions(actions: => Batch[GlobalEvent]): Input =
    this.copy(onFocus = () => actions)

  def withLoseFocusActions(actions: GlobalEvent*): Input =
    withLoseFocusActions(Batch.fromSeq(actions))
  def withLoseFocusActions(actions: => Batch[GlobalEvent]): Input =
    this.copy(onLoseFocus = () => actions)

  // Delegates, for convenience.

  def update[StartupData, ContextData](
      context: UiContext[?]
  ): GlobalEvent => Outcome[Input] =
    summon[Component[Input, ?]].updateModel(context, this)

  def present[StartupData, ContextData](
      context: UiContext[?]
  ): Outcome[ComponentFragment] =
    summon[Component[Input, ?]].present(context, this)

  def reflow: Input =
    summon[Component[Input, ?]].reflow(this)

  def cascade(parentBounds: Bounds): Input =
    summon[Component[Input, ?]].cascade(this, parentBounds)

object Input:

  /** Minimal input constructor with custom rendering function
    */
  def apply(bounds: Bounds)(
      present: (Coords, Bounds, Input, Seconds) => Outcome[ComponentFragment]
  ): Input =
    Input(
      "",
      bounds,
      present,
      _ => Batch.empty,
      //
      characterLimit = bounds.width,
      cursor = Cursor.default,
      hasFocus = false,
      () => Batch.empty,
      () => Batch.empty
    )

  private val graphic = Graphic(0, 0, TerminalMaterial(AssetName(""), RGBA.White, RGBA.Black))

  private def presentInput[ReferenceData](
      charSheet: CharSheet,
      fgColor: RGBA,
      bgColor: RGBA
  ): (Coords, Bounds, Input, Seconds) => Outcome[ComponentFragment] =
    (offset, bounds, input, runningTime) =>
      val correctedLabel =
        if input.text.length == bounds.width then input.text
        else if input.text.length > bounds.width then input.text.take(bounds.width)
        else input.text + (List.fill(bounds.width - input.text.length)(" ").mkString)

      val hBar = Batch.fill(correctedLabel.length)("─").mkString
      val size = (bounds.dimensions + 2).unsafeToSize

      val terminal =
        RogueTerminalEmulator(size)
          .put(Point(0, 0), Tile.`┌`, fgColor, bgColor)
          .put(Point(size.width - 1, 0), Tile.`┐`, fgColor, bgColor)
          .put(Point(0, size.height - 1), Tile.`└`, fgColor, bgColor)
          .put(Point(size.width - 1, size.height - 1), Tile.`┘`, fgColor, bgColor)
          .put(Point(0, 1), Tile.`│`, fgColor, bgColor)
          .put(Point(size.width - 1, 1), Tile.`│`, fgColor, bgColor)
          .putLine(Point(1, 0), hBar, fgColor, bgColor)
          .putLine(
            Point(1, 1),
            correctedLabel,
            fgColor,
            bgColor
          )
          .putLine(Point(1, 2), hBar, fgColor, bgColor)

      val terminalClones =
        terminal
          .toCloneTiles(
            CloneId(s"input_${charSheet.assetName.toString}"),
            bounds.coords
              .toScreenSpace(charSheet.size)
              .moveBy(offset.toScreenSpace(charSheet.size)),
            charSheet.charCrops
          ) { case (fg, bg) =>
            graphic.withMaterial(TerminalMaterial(charSheet.assetName, fg, bg))
          }

      val cursor: Batch[SceneNode] =
        if input.hasFocus then
          input.cursor.cursorBlinkRate match
            case None =>
              Batch(
                Shape.Box(
                  Rectangle(
                    0,
                    0,
                    Math.max(1, charSheet.size.width / 5).toInt,
                    charSheet.size.height
                  ),
                  Fill.Color(fgColor)
                )
              )

            case Some(blinkRate) =>
              Signal
                .Pulse(blinkRate)
                .map(p => if (runningTime - input.cursor.lastCursorMove < Seconds(0.5)) true else p)
                .map {
                  case false =>
                    Batch.empty

                  case true =>
                    Batch(
                      Shape.Box(
                        Rectangle(
                          offset.toScreenSpace(charSheet.size),
                          Size(
                            Math.max(1, charSheet.size.width / 5).toInt,
                            charSheet.size.height
                          )
                        ),
                        Fill.Color(fgColor)
                      )
                    )
                }
                .at(runningTime)
        else Batch.empty

      Outcome(ComponentFragment(terminalClones).addNodes(cursor))

  /** Creates a Input rendered using the RogueTerminalEmulator based on a `Input.Theme`, where the
    * bounds are the supplied width, height 1, plus border.
    */
  def apply(width: Int, theme: Theme): Input =
    Input(
      "",
      Bounds(0, 0, width, 1),
      presentInput(theme.charSheet, theme.colors.foreground, theme.colors.background),
      _ => Batch.empty,
      //
      characterLimit = width,
      cursor = Cursor.default,
      hasFocus = false,
      () => Batch.empty,
      () => Batch.empty
    )

  given [ReferenceData]: Component[Input, ReferenceData] with
    def bounds(model: Input): Bounds =
      model.bounds

    def updateModel(
        context: UiContext[ReferenceData],
        model: Input
    ): GlobalEvent => Outcome[Input] =
      case _: MouseEvent.Click
          if model.bounds
            .resizeBy(2, 2)
            .moveBy(context.bounds.coords)
            .contains(context.mouseCoords) =>
        Outcome(model.copy(hasFocus = true))

      case _: MouseEvent.Click =>
        Outcome(model.copy(hasFocus = false))

      case KeyboardEvent.KeyUp(Key.BACKSPACE) if model.hasFocus =>
        val next = model.backspace.withLastCursorMove(context.running)
        Outcome(next, model.change(next.text))

      case KeyboardEvent.KeyUp(Key.DELETE) if model.hasFocus =>
        val next = model.delete.withLastCursorMove(context.running)
        Outcome(next, model.change(next.text))

      case KeyboardEvent.KeyUp(Key.LEFT_ARROW) if model.hasFocus =>
        Outcome(model.cursorLeft.withLastCursorMove(context.running))

      case KeyboardEvent.KeyUp(Key.RIGHT_ARROW) if model.hasFocus =>
        Outcome(model.cursorRight.withLastCursorMove(context.running))

      case KeyboardEvent.KeyUp(Key.HOME) if model.hasFocus =>
        Outcome(model.cursorHome.withLastCursorMove(context.running))

      case KeyboardEvent.KeyUp(Key.END) if model.hasFocus =>
        Outcome(model.cursorEnd.withLastCursorMove(context.running))

      case KeyboardEvent.KeyUp(Key.ENTER) if model.hasFocus =>
        Outcome(model.withLastCursorMove(context.running))

      case KeyboardEvent.KeyUp(key) if model.hasFocus && key.isPrintable =>
        val next = model.addCharacterText(key.key).withLastCursorMove(context.running)
        Outcome(next, model.change(next.text))

      case _ =>
        Outcome(model)

    def present(
        context: UiContext[ReferenceData],
        model: Input
    ): Outcome[ComponentFragment] =
      model.render(
        context.bounds.coords,
        model.bounds,
        model,
        context.running
      )

    def reflow(model: Input): Input =
      model

    def cascade(model: Input, parentBounds: Bounds): Input =
      model

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

final case class Cursor(
    cursorPosition: Int,
    cursorBlinkRate: Option[Seconds],
    lastCursorMove: Seconds
):

  def moveTo(position: Int): Cursor =
    this.copy(cursorPosition = position)

  def noCursorBlink: Cursor =
    this.copy(cursorBlinkRate = None)
  def withCursorBlinkRate(interval: Seconds): Cursor =
    this.copy(cursorBlinkRate = Some(interval))

  def withLastCursorMove(value: Seconds): Cursor =
    this.copy(lastCursorMove = value)

  def cursorLeft: Cursor =
    this.copy(cursorPosition = if (cursorPosition - 1 >= 0) cursorPosition - 1 else cursorPosition)

  def cursorRight(maxLength: Int): Cursor =
    this.copy(cursorPosition =
      if (cursorPosition + 1 <= maxLength) cursorPosition + 1 else maxLength
    )

  def cursorHome: Cursor =
    this.copy(cursorPosition = 0)

  def moveCursorTo(newCursorPosition: Int, maxLength: Int): Cursor =
    if newCursorPosition >= 0 && newCursorPosition < maxLength then
      this.copy(cursorPosition = newCursorPosition)
    else if newCursorPosition < 0 then this.copy(cursorPosition = 0)
    else this.copy(cursorPosition = maxLength - 1)

  def cursorEnd(maxLength: Int): Cursor =
    this.copy(cursorPosition = maxLength)

object Cursor:
  val default: Cursor =
    Cursor(0, Option(Seconds(0.5)), Seconds.zero)
