package infra

import cats.effect.concurrent.Deferred
import cats.effect.{IO, Sync}
import cats.implicits._
import model.{PercentageReleaseRpsRule, Sla}

import scala.collection.immutable.HashMap
import scala.concurrent.duration._

class ThrottlingServiceIT extends IntegrationTest {

  behavior of "infra.ThrottlingService"

  private val slaService = new SlaService[IO] {
    private val usersSla = HashMap[String, Sla](
      "token1" -> Sla("user1", 10),
      "token2" -> Sla("user2", 6),
      "token3" -> Sla("user3", 6),
      "token4" -> Sla("user4", 10)
    )

    //TODO unhandled case when there is no such token
    override def getSlaByToken(token: String): IO[Sla] = IO.pure(usersSla(token))
  }
  private val graceRps = 10
  private val releasePercentage = 10
  private val users = List("token1", "token2", "token3", "token4")
  private val userRps = 2
  private val runTime = 11.seconds

  private val timeIntervalRpsRule = new PercentageReleaseRpsRule(releasePercentage)
  private val rpsCacheService = new CacheRpsService[IO](timeIntervalRpsRule)
  private val thrService = new CachedThrottlingService[IO](graceRps, slaService, rpsCacheService)

  //Streams
  private val updateRpsService = new TimeIntervalRpsUpdateService(100.milliseconds, 1.seconds, rpsCacheService)
  private val requestCallGenerator = fs2.Stream.awakeDelay[IO](1.seconds).evalMap { _ =>
    users.flatTraverse { token =>
      (1 to userRps).toList.traverse(_ => thrService.isRequestAllowed(Some(token)))
    }
  }.interruptAfter(runTime)


  //TODO refactoring required, should avoid var, stream can be wrapped in service, and correctly pass throtlling,
  //TODO but it's required more time than 4 hours
  def dummyEndpointService(throtlling: Option[ThrottlingService[IO]], token: String): fs2.Stream[IO, Unit] = {
    val requestNeedMade = 15
    var currentRequestMade = 0

    fs2.Stream
      .eval(Deferred[IO, Unit])
      .flatMap { switch =>
        val updateCacheRpsStream = updateRpsService.updateRps() merge updateRpsService.reset()
        val requestsStream = fs2.Stream.repeatEval {
          for {
            allowed <- throtlling.fold(IO.pure(true))(_.isRequestAllowed(Some(token)))
            _ <- Sync[IO].whenA(allowed) (IO.delay {currentRequestMade += 1})
            _ <- Sync[IO].whenA(currentRequestMade >= requestNeedMade)(switch.complete())
          } yield ()
        }.metered(500.milliseconds)

        requestsStream.concurrently(updateCacheRpsStream).interruptWhen(switch.get.attempt)
      }
  }

  it should "verify that N*K*T request are successful" in {
    //When
    val updateCacheRpsStream = updateRpsService.updateRps() merge updateRpsService.reset()
    val requestMade = requestCallGenerator
      .concurrently(updateCacheRpsStream)
      .compile
      .toList
      .unsafeRunSync()
      .flatten

    //Then
    val allowedRequest = requestMade.count(identity)
    // Minus 1, because stream will awake after 1 sec, so actual run time is 10 sec
    val T = runTime.toSeconds.toInt - 1
    val K = userRps
    val N = users.length
    allowedRequest should be >= (T * K * N)
  }

  it should "compare time with and without infra.ThrottlingService" in {
    //Given

    val token = "token1"
    val timeStart = System.currentTimeMillis()
    dummyEndpointService(Some(thrService), token).compile.drain.unsafeRunSync()

    val executionTimeWithThrottling = System.currentTimeMillis() - timeStart

    val timeStart2 = System.currentTimeMillis()

    dummyEndpointService(None, token).compile.drain.unsafeRunSync()

    val executionTimeWithoutThrottling = System.currentTimeMillis() - timeStart2

    println(s"Execution time with Throttling: ${executionTimeWithThrottling}")
    println(s"Execution time without Throttling: ${executionTimeWithoutThrottling}")

    executionTimeWithThrottling should be > executionTimeWithoutThrottling
  }


}
