package infra

import cats.effect.{ContextShift, IO, Timer}

trait IntegrationTest extends UnitTest {
  implicit protected val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
  implicit protected val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)

}
