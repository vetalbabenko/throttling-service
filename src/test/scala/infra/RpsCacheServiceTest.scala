package infra

import cats.effect.IO
import model.{PercentageReleaseRpsRule, Sla}

class RpsCacheServiceTest extends UnitTest {

  behavior of "RpsCacheService"

  it should "return 'true' if user in range of allowed rps" in {
    //Given
    val rpsRule = new PercentageReleaseRpsRule(10)
    val rpsCacheService = new CacheRpsService[IO](rpsRule)
    val sla1 = Sla("user1", 3)

    //When
    val checks = (1 to 3).map(_ => rpsCacheService.checkRps(sla1).unsafeRunSync())

    //Then
    checks should be (List(true, true, true))
  }

  it should "return 'false' if user is reached rps limit" in {
    //Given
    val rpsRule = new PercentageReleaseRpsRule(10)
    val rpsCacheService = new CacheRpsService[IO](rpsRule)
    val sla1 = Sla("user1", 3)

    //When
    val checks = (1 to 5).map(_ => rpsCacheService.checkRps(sla1).unsafeRunSync())

    //Then
    checks should be (List(true, true, true, false, false))
  }

  it should "correctly update rps based of given rule and allow request if it's below limit after release" in {
    //Given
    val rpsRule = new PercentageReleaseRpsRule(50)
    val rpsCacheService = new CacheRpsService[IO](rpsRule)
    val sla1 = Sla("user1", 10)

    //When
    (1 to 10).map(_ => rpsCacheService.checkRps(sla1).unsafeRunSync())

    val notAllowed = rpsCacheService.checkRps(sla1).unsafeRunSync()
    rpsCacheService.updateRps().unsafeRunSync()
    val allowed = rpsCacheService.checkRps(sla1).unsafeRunSync()

    //Then
    notAllowed should be (false)
    allowed should be (true)
  }

  it should "correctly update rps based of given rule and not allow request if it's above limit after release" in {
    //Given
    val rpsRule = new PercentageReleaseRpsRule(10)
    val rpsCacheService = new CacheRpsService[IO](rpsRule)
    val sla1 = Sla("user1", 10)

    //When
    (1 to 15).map(_ => rpsCacheService.checkRps(sla1).unsafeRunSync())

    val notAllowed = rpsCacheService.checkRps(sla1).unsafeRunSync()
    rpsCacheService.updateRps().unsafeRunSync()
    val allowed = rpsCacheService.checkRps(sla1).unsafeRunSync()

    //Then
    notAllowed should be (false)
    allowed should be (false)
  }

}
