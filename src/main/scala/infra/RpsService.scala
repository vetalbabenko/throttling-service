package infra

import cats.effect.Sync
import model.{RpsRule, Sla}

import scala.collection.concurrent.TrieMap

trait RpsService[F[_]] {
  def checkRps(sla: Sla): F[Boolean]
  def updateRps(): F[Unit]
  def reset(): F[Unit]
}

class CacheRpsService[F[_]: Sync](releaseRpsRule: RpsRule) extends RpsService[F] {
  private val cachedRps: TrieMap[Sla, Long] = TrieMap.empty[Sla, Long]

  def checkRps(sla: Sla): F[Boolean] = Sync[F].delay{
    val rpsMade = cachedRps.get(sla).fold(0L)(identity) + 1
    cachedRps.update(sla, rpsMade)
    rpsMade <= sla.rps
  }

  def updateRps(): F[Unit] = Sync[F].delay{
    val reachedLimitRps =  cachedRps.filter{case (sla, currentRps) => currentRps >= sla.rps }

    reachedLimitRps.foreach{case (sla, currentRps) =>
      cachedRps.update(sla, releaseRpsRule(currentRps))
    }
  }

  def reset(): F[Unit] = Sync[F].delay(cachedRps.clear())
}
