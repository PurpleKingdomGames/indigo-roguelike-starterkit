package demo.models

import demo.Assets
import indigo.*
import indigoextras.ui.*
import roguelikestarterkit.*
import roguelikestarterkit.ui.*

final case class Model(
    pointerOverWindows: Batch[WindowId],
    num: Int,
    components: ComponentGroup[Int]
)

object Model:

  import indigo.syntax.*

  val defaultCharSheet: CharSheet =
    CharSheet(
      Assets.assets.AnikkiSquare10x10,
      Size(10),
      RoguelikeTiles.Size10x10.charCrops,
      RoguelikeTiles.Size10x10.Fonts.fontKey
    )

  val initial: Model =
    Model(
      Batch.empty,
      0,
      ComponentGroup(BoundsMode.fixed(200, 300))
        .add(
          ComponentList(Dimensions(200, 40)) { (_: Int) =>
            (1 to 3).toBatch.map { i =>
              ComponentId("lbl" + i) -> Label[Int](
                "Custom rendered label " + i,
                (_, label) => Bounds(0, 0, 150, 10)
              ) { case (offset, label, dimensions) =>
                Outcome(
                  Layer(
                    TextBox(label)
                      .withColor(RGBA.Red)
                      .moveTo(offset.unsafeToPoint)
                      .withSize(dimensions.unsafeToSize)
                  )
                )
              }
            }
          }
        )
        .add(
          Label[Int](
            "Another label",
            (_, label) => Bounds(0, 0, 150, 10)
          ) { case (offset, label, dimensions) =>
            Outcome(
              Layer(
                TextBox(label)
                  .withColor(RGBA.White)
                  .moveTo(offset.unsafeToPoint)
                  .withSize(dimensions.unsafeToSize)
              )
            )
          }
        )
        .add(
          Switch[Int, Int](BoundsType.fixed(40, 40))(
            (coords, bounds, _) =>
              Outcome(
                Layer(
                  Shape
                    .Box(
                      bounds.unsafeToRectangle,
                      Fill.Color(RGBA.Green.mix(RGBA.Black)),
                      Stroke(1, RGBA.Green)
                    )
                    .moveTo(coords.unsafeToPoint)
                )
              ),
            (coords, bounds, _) =>
              Outcome(
                Layer(
                  Shape
                    .Box(
                      bounds.unsafeToRectangle,
                      Fill.Color(RGBA.Red.mix(RGBA.Black)),
                      Stroke(1, RGBA.Red)
                    )
                    .moveTo(coords.unsafeToPoint)
                )
              )
          )
            .onSwitch(value => Batch(Log("Switched to: " + value)))
            .switchOn
        )
        .add(
          Button[Int](Bounds(32, 32)) { (coords, bounds, _) =>
            Outcome(
              Layer(
                Shape
                  .Box(
                    bounds.unsafeToRectangle,
                    Fill.Color(RGBA.Magenta.mix(RGBA.Black)),
                    Stroke(1, RGBA.Magenta)
                  )
                  .moveTo(coords.unsafeToPoint)
              )
            )
          }
            .presentDown { (coords, bounds, _) =>
              Outcome(
                Layer(
                  Shape
                    .Box(
                      bounds.unsafeToRectangle,
                      Fill.Color(RGBA.Cyan.mix(RGBA.Black)),
                      Stroke(1, RGBA.Cyan)
                    )
                    .moveTo(coords.unsafeToPoint)
                )
              )
            }
            .presentOver((coords, bounds, _) =>
              Outcome(
                Layer(
                  Shape
                    .Box(
                      bounds.unsafeToRectangle,
                      Fill.Color(RGBA.Yellow.mix(RGBA.Black)),
                      Stroke(1, RGBA.Yellow)
                    )
                    .moveTo(coords.unsafeToPoint)
                )
              )
            )
            .onClick(Log("Button clicked"))
            .onPress(Log("Button pressed"))
            .onRelease(Log("Button released"))
        )
        .add(
          ComponentList(Dimensions(200, 150)) { (_: Int) =>
            (1 to 3).toBatch.map { i =>
              ComponentId("radio-" + i) ->
                ComponentGroup(BoundsMode.fixed(200, 30))
                  .withLayout(ComponentLayout.Horizontal(Padding.right(10)))
                  .add(
                    Switch[Int, Int](BoundsType.fixed(20, 20))(
                      (coords, bounds, _) =>
                        Outcome(
                          Layer(
                            Shape
                              .Circle(
                                bounds.unsafeToRectangle.toIncircle,
                                Fill.Color(RGBA.Green.mix(RGBA.Black)),
                                Stroke(1, RGBA.Green)
                              )
                              .moveTo(coords.unsafeToPoint + Point(10))
                          )
                        ),
                      (coords, bounds, _) =>
                        Outcome(
                          Layer(
                            Shape
                              .Circle(
                                bounds.unsafeToRectangle.toIncircle,
                                Fill.Color(RGBA.Red.mix(RGBA.Black)),
                                Stroke(1, RGBA.Red)
                              )
                              .moveTo(coords.unsafeToPoint + Point(10))
                          )
                        )
                    )
                      .onSwitch { value =>
                        Batch(
                          Log("Selected: " + i),
                          ChangeValue(i)
                        )
                      }
                      .withAutoToggle { (_, ref) =>
                        if ref == i then Option(SwitchState.On) else Option(SwitchState.Off)
                      }
                  )
                  .add(
                    Label[Int](
                      "Radio " + i,
                      (_, label) => Bounds(0, 0, 150, 10)
                    ) { case (offset, label, dimensions) =>
                      Outcome(
                        Layer(
                          TextBox(label)
                            .withColor(RGBA.Red)
                            .moveTo(offset.unsafeToPoint)
                            .withSize(dimensions.unsafeToSize)
                        )
                      )
                    }
                  )
            }
          }
        )
    )

final case class ChangeValue(value: Int) extends GlobalEvent
