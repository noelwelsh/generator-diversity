package generator

import scala.util.Random

extension [A](generator: Generator[A]) {
  def random(r: Random = Random): A =
    generator match {
      case Generator.Pure(value)        => value
      case Generator.FlatMap(source, f) => f(source.random(r)).random(r)
      case Generator.Choose(choices) =>
        val l = choices.size
        val c = r.between(0, l)
        choices(c).random(r)
      case Generator.Delay(generator) => generator().random(r)
    }
}
