package generator

object Examples {
  val oneToFour: Generator[Int] = Generator.chooseValues(1, 2, 3, 4)

  val intList: Generator[List[Int]] =
    Generator.choose(
      Generator.pure(Nil),
      for {
        hd <- oneToFour
        tl <- Generator.delay(intList)
      } yield hd :: tl
    )
}
