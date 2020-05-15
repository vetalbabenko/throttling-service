package infra

import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait UnitTest extends AnyFlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach
