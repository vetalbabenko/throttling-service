package infra

import model.Sla

trait SlaService[F[_]] {
  def getSlaByToken(token: String): F[Sla]
}
