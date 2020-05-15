package infra

import cats.effect.{Sync, Timer}

import scala.concurrent.duration.FiniteDuration

trait RpsUpdateService[F[_]] {
  def updateRps(): fs2.Stream[F, Unit]
  def reset(): fs2.Stream[F, Unit]
}

class TimeIntervalRpsUpdateService[F[_]: Sync: Timer](
  updateInterval: FiniteDuration,
  resetInterval: FiniteDuration,
  rpsService: RpsService[F]
) extends RpsUpdateService[F] {

  override def updateRps(): fs2.Stream[F, Unit] = {
    fs2.Stream.awakeDelay[F](updateInterval).evalMap(_ => rpsService.updateRps())
  }

  override def reset(): fs2.Stream[F, Unit] =
    fs2.Stream.awakeDelay[F](resetInterval).evalMap(_ => rpsService.reset())

}
