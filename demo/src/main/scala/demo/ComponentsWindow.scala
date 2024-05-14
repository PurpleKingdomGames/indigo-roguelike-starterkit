package demo

import indigo.*
import roguelikestarterkit.*

object ComponentsWindow:

  private val graphic = Graphic(0, 0, TerminalMaterial(AssetName(""), RGBA.White, RGBA.Black))

  val windowId: WindowId = WindowId("ComponentsWindow")

  def window(
      charSheet: CharSheet
  ): WindowModel[ComponentGroup, Unit] =
    WindowModel(
      windowId,
      charSheet,
      ComponentGroup(Bounds(0, 0, 23, 23))
        .withLayout(ComponentLayout.Vertical(Padding(1)))
        .inheritBounds
        .add(
          Button(
            "Hello!",
            Button.Theme(
              charSheet,
              RGBA.Silver -> RGBA.Black,
              RGBA.White  -> RGBA.Black,
              RGBA.Black  -> RGBA.White,
              hasBorder = true
            )
          )
        )
        .add(
          Button(
            "World!",
            Button.Theme(
              charSheet,
              RGBA.Silver -> RGBA.Black,
              RGBA.Green  -> RGBA.Black,
              RGBA.Black  -> RGBA.Yellow,
              hasBorder = false
            )
          )
        )
        .add(
          Button(Bounds(0, 0, 5, 2)) { case (coords, bounds) =>
            Outcome(
              ComponentFragment(
                Shape.Box(
                  bounds.toScreenSpace(charSheet.size).moveTo(coords.toScreenSpace(charSheet.size)),
                  Fill.LinearGradient(Point.zero, RGBA.Cyan, Point(50, 0), RGBA.Magenta)
                )
              )
            )
          }
        )
    )
      .withTitle("Colour Palette")
      .moveTo(0, 0)
      .resizeTo(25, 25)
      .isDraggable
      .isResizable
      .isCloseable
