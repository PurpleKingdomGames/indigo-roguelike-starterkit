package demo

import indigo.*
import indigo.scenes.*
import io.indigoengine.roguelike.starterkit.*

object TerminalTextScene extends Scene[Unit, Unit, Unit]:

  type SceneModel     = Unit
  type SceneViewModel = Unit

  val name: SceneName =
    SceneName("TerminalText scene")

  val modelLens: Lens[Unit, Unit] =
    Lens.keepLatest

  val viewModelLens: Lens[Unit, Unit] =
    Lens.keepLatest

  val eventFilters: EventFilters =
    EventFilters.Permissive

  val subSystems: Set[SubSystem] =
    Set()

  def updateModel(context: SceneContext[Unit], model: Unit): GlobalEvent => Outcome[Unit] =
    case KeyboardEvent.KeyUp(Key.SPACE) =>
      Outcome(model).addGlobalEvents(SceneEvent.JumpTo(TerminalEmulatorScene.name))

    case _ =>
      Outcome(model)

  def updateViewModel(
      context: SceneContext[Unit],
      model: Unit,
      viewModel: Unit
  ): GlobalEvent => Outcome[Unit] =
    _ => Outcome(viewModel)

  val size = Size(30)

  def message: String =
    """
    |╔═════════════════════╗
    |║ Hit Space to Start! ║
    |╚═════════════════════╝
    |""".stripMargin

  def present(
      context: SceneContext[Unit],
      model: Unit,
      viewModel: Unit
  ): Outcome[SceneUpdateFragment] =
    Outcome(
      SceneUpdateFragment(
        Text(
          message,
          RoguelikeTiles.Size10x10.Fonts.fontKey,
          TerminalText(Assets.tileMap, RGB.Cyan, RGBA.Blue)
        ),
        Text(
          message,
          RoguelikeTiles.Size10x10.Fonts.fontKey,
          TerminalText(Assets.tileMap, RGB.Yellow, RGBA.Red).withShaderId(ShaderId("my shader"))
        ).moveBy(0, 40)
      )
    )
