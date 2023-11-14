package io.indigoengine.roguelike.starterkit.utils

import indigo.shared.collections.Batch
import indigo.shared.datatypes.Point
import indigo.shared.datatypes.Size
import indigo.shared.dice.Dice

import scala.annotation.tailrec

final case class PathFinder(size: Size, grid: Batch[GridSquare]):

  def contains(coords: Point): Boolean =
    coords.x >= 0 && coords.y >= 0 && coords.x < size.width && coords.y < size.height

  def locatePath(
      dice: Dice,
      start: Point,
      end: Point,
      scoreAmount: GridSquare => Int
  ): Batch[Point] =
    PathFinder.locatePath(
      dice,
      start,
      end,
      this.copy(grid = PathFinder.scoreGridSquares(start: Point, end: Point, this, scoreAmount))
    )

object PathFinder:

  def sampleAt(searchGrid: PathFinder, coords: Point, gridWidth: Int): Batch[GridSquare] =
    Batch(
      coords + GridSquare.relativeUp,
      coords + GridSquare.relativeLeft,
      coords + GridSquare.relativeRight,
      coords + GridSquare.relativeDown
    ).filter(c => searchGrid.contains(c))
      .map(c => searchGrid.grid(GridSquare.toIndex(c, gridWidth)))

  def fromImpassable(size: Size, impassable: Batch[Point]): PathFinder =
    val grid: Batch[GridSquare] =
      Batch.fromIndexedSeq(0 until (size.width * size.height)).map { index =>
        GridSquare.fromIndex(index, size.width) match
          case c: Point if impassable.contains(c) =>
            GridSquare.Blocked(index, c)

          case c: Point =>
            GridSquare.Walkable(index, c, -1)
      }

    PathFinder(size, grid)

  def fromWalkable(size: Size, walkable: Batch[Point]): PathFinder =
    val grid: Batch[GridSquare] =
      Batch.fromIndexedSeq(0 until (size.width * size.height)).map { index =>
        GridSquare.fromIndex(index, size.width) match
          case c: Point if walkable.contains(c) =>
            GridSquare.Walkable(index, c, -1)

          case c: Point =>
            GridSquare.Blocked(index, c)
      }

    PathFinder(size, grid)

  def scoreGridSquares(
      start: Point,
      end: Point,
      searchGrid: PathFinder,
      scoreAmount: GridSquare => Int
  ): Batch[GridSquare] = {
    @tailrec
    def rec(
        target: Point,
        unscored: Batch[GridSquare],
        scoreValue: Int,
        lastCoords: Batch[Point],
        scored: Batch[GridSquare]
    ): Batch[GridSquare] =
      (unscored, lastCoords) match
        case (a, b) if a.isEmpty || b.isEmpty =>
          scored ++ unscored

        case (_, last) if last.exists(_ == target) =>
          scored ++ unscored

        case (remainingSquares, lastScoredLocations) =>
          // Find the squares from the remaining pile that the previous scores squares touched.
          val roughEdges: Batch[Batch[GridSquare]] =
            lastScoredLocations.map(c => sampleAt(searchGrid, c, searchGrid.size.width))

          // Filter out any squares that aren't in the remainingSquares list
          val edges: Batch[GridSquare] =
            roughEdges.flatMap(_.filter(c => remainingSquares.contains(c)))

          // Deduplicate and score
          val next: Batch[GridSquare] =
            edges
              .foldLeft(Batch.empty[GridSquare]) { (l, x) =>
                if (l.exists(p => p.coords == x.coords)) l else l ++ Batch(x)
              }
              .map(gs => gs.withScore(scoreValue + scoreAmount(gs)))

          rec(
            target = target,
            unscored = remainingSquares.filterNot(p => next.exists(q => q.coords == p.coords)),
            scoreValue = scoreValue + 1,
            lastCoords = next.map(_.coords),
            scored = next ++ scored
          )

    val (done, todo) = searchGrid.grid.partition(_.coords == end)

    rec(start, todo, 0, Batch(end), done.map(_.withScore(0))).sortBy(_.index)
  }

  def locatePath(dice: Dice, start: Point, end: Point, searchGrid: PathFinder): Batch[Point] = {
    val width: Int = searchGrid.size.width

    @tailrec
    def rec(
        currentPosition: Point,
        currentScore: Int,
        acc: Batch[Point]
    ): Batch[Point] =
      if (currentPosition == end) acc
      else
        val squares = sampleAt(searchGrid, currentPosition, width).filter(c =>
          c.score != -1 && c.score < currentScore
        )

        if squares.isEmpty then acc
        else if squares.size == 1 then
          val next = squares.head
          rec(next.coords, next.score, acc ++ Batch(next.coords))
        else
          val next = squares(dice.rollFromZero(squares.length - 1))
          rec(next.coords, next.score, acc ++ Batch(next.coords))

    rec(
      start,
      GridSquare.Max,
      Batch(start)
    )
  }

sealed trait GridSquare:
  val index: Int
  val coords: Point
  val score: Int
  def withScore(score: Int): GridSquare

object GridSquare:
  val Max: Int = Int.MaxValue

  final case class Walkable(index: Int, coords: Point, score: Int) extends GridSquare:
    def withScore(newScore: Int): Walkable = this.copy(score = newScore)

  final case class Blocked(index: Int, coords: Point) extends GridSquare:
    val score: Int                     = Int.MaxValue
    def withScore(score: Int): Blocked = this

  val relativeUpLeft: Point    = Point(-1, -1)
  val relativeUp: Point        = Point(0, -1)
  val relativeUpRight: Point   = Point(1, -1)
  val relativeLeft: Point      = Point(-1, 0)
  val relativeRight: Point     = Point(1, 0)
  val relativeDownLeft: Point  = Point(-1, 1)
  val relativeDown: Point      = Point(0, 1)
  val relativeDownRight: Point = Point(1, 1)

  def toIndex(coords: Point, gridWidth: Int): Int =
    coords.x + (coords.y * gridWidth)

  def fromIndex(index: Int, gridWidth: Int): Point =
    Point(
      x = index % gridWidth,
      y = index / gridWidth
    )

  def add(a: Point, b: Point): Point =
    Point(a.x + b.x, a.y + b.y)
