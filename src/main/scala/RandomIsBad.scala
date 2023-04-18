import cats.syntax.all.*
import doodle.core.*
import doodle.core.format._
import doodle.syntax.all.*
import doodle.java2d.*
import doodle.random.*
import cats.effect.unsafe.implicits.global

object RandomIsBad {
  val lightness = 0.7
  val hue = Random.double.map(x => (x * 360.0).degrees)
  val saturation = Random.double

  val color = (hue, saturation).mapN((h, s) => Color.hsl(h, s, lightness))

  def nColors(n: Int): Random[List[Color]] = color.replicateA(n)

  def plot(colors: List[Color]): Picture[Unit] =
    colors
      .map(c =>
        Picture.circle(10).fillColor(c).at(c.saturation.get * 200, c.hue)
      )
      .allOn
      .on(Picture.circle(400))

  val example =
    (nColors(100), nColors(1000)).mapN((small, large) =>
      plot(small).margin(0, 10, 0, 0).beside(plot(large))
    )

  @main def go(): Unit =
    example.run.write[Pdf]("paper/random-is-bad.pdf")
}
