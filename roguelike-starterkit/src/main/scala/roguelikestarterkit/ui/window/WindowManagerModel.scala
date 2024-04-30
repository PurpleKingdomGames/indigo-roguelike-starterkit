package roguelikestarterkit.ui.window

import indigo.*
import roguelikestarterkit.ui.datatypes.Bounds
import roguelikestarterkit.ui.datatypes.Coords
import roguelikestarterkit.ui.datatypes.Dimensions
import roguelikestarterkit.ui.datatypes.UiContext

final case class WindowManagerModel[A](windows: Batch[WindowModel[_]]):
  def register(windowModels: WindowModel[_]*): WindowManagerModel[A] =
    register(Batch.fromSeq(windowModels))
  def register(
      windowModels: Batch[WindowModel[_]]
  ): WindowManagerModel[A] =
    this.copy(windows = windows ++ windowModels)

  def open(ids: WindowId*): WindowManagerModel[A] =
    open(Batch.fromSeq(ids))

  def open(ids: Batch[WindowId]): WindowManagerModel[A] =
    this.copy(windows = windows.map(w => if ids.exists(_ == w.id) then w.open else w))

  def close(id: WindowId): WindowManagerModel[A] =
    this.copy(windows = windows.map(w => if w.id == id then w.close else w))

  def giveFocusAndSurfaceAt(coords: Coords): WindowManagerModel[A] =
    val reordered =
      windows.reverse.find(w => !w.static && w.bounds.contains(coords)) match
        case None =>
          windows

        case Some(w) =>
          windows.filterNot(_.id == w.id).map(_.blur) :+ w.focus

    this.copy(windows = reordered)

  def moveTo(id: WindowId, position: Coords): WindowManagerModel[A] =
    this.copy(windows = windows.map(w => if w.id == id then w.moveTo(position) else w))

  def resizeTo(id: WindowId, dimensions: Dimensions): WindowManagerModel[A] =
    this.copy(windows = windows.map(w => if w.id == id then w.resizeTo(dimensions) else w))

  def transformTo(id: WindowId, bounds: Bounds): WindowManagerModel[A] =
    this.copy(windows =
      windows.map(w =>
        // Note: We do _not_ use .withBounds here because that won't do the min size checks.
        if w.id == id then w.moveTo(bounds.coords).resizeTo(bounds.dimensions) else w
      )
    )

  def update(
      context: UiContext,
      event: GlobalEvent
  ): Outcome[WindowManagerModel[A]] =
    WindowManager.updateModel(context, this)(event)

object WindowManagerModel:
  def initial[A]: WindowManagerModel[A] =
    WindowManagerModel(Batch.empty)
