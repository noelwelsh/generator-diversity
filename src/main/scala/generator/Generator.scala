package generator

enum Generator[A] {
  case Pure(value: A)
  case FlatMap[A, B](source: Generator[A], f: A => Generator[B])
      extends Generator[B]
  case Choose(choices: Seq[Generator[A]])
  case Delay(generator: () => Generator[A])

  def flatMap[B](f: A => Generator[B]): Generator[B] =
    FlatMap(this, f)
  def map[B](f: A => B): Generator[B] =
    FlatMap(this, a => Pure(f(a)))
}
object Generator {
  def choose[A](choice: Generator[A], choices: Generator[A]*): Generator[A] =
    Generator.Choose(choice +: choices)

  def chooseValues[A](value: A, values: A*): Generator[A] =
    Generator.Choose((value +: values).map(a => pure(a)))

  def delay[A](generator: => Generator[A]): Generator[A] =
    Generator.Delay(() => generator)

  def pure[A](value: A): Generator[A] =
    Generator.Pure(value)
}
