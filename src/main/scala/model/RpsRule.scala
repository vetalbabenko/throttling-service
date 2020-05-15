package model

trait RpsRule extends (Long => Long) {
}

class PercentageReleaseRpsRule(releasePercentage: Long) extends RpsRule {
  override def apply(v1: Long): Long = ((1 - releasePercentage/100D) * v1).toLong
}
