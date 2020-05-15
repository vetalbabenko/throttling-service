package infra

import cats.effect.Sync
import cats.implicits._
import model.Sla

trait ThrottlingService[F[_]] {
  val graceRps: Int // configurable
  val slaService: SlaService[F] // use mocks/stubs for testing
  // Should return true if the request is within allowed RPS.
  def isRequestAllowed(token: Option[String]): F[Boolean]
}

class CachedThrottlingService[F[_]: Sync](
  val graceRps: Int,
  val slaService: SlaService[F],
  rpsCacheService: RpsService[F]
) extends ThrottlingService[F] {

  type Token = String
  type User = String

  private val cachedUsers = scala.collection.concurrent.TrieMap.empty[Token, Sla]

  def isRequestAllowed(token: Option[String]): F[Boolean] = {
    token match {
      case Some(token) =>
        cachedUsers.get(token).fold(slaService.getSlaByToken(token))(Sync[F].pure)
          .flatMap { sla =>
            rpsCacheService.checkRps(sla).flatTap(allowed => Sync[F].delay(println(s"Request for $token ${if(allowed) "allowed" else "not allowed"}")))

          }
          .recover { case e => println(s"Fetching data from SLA server failed: $e"); false }
      case None => rpsCacheService.checkRps(Sla("Unauthorized", graceRps))
    }
  }
}