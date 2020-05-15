package infra

import model.PercentageReleaseRpsRule

class PercentageReleaseRpsRuleTest extends UnitTest {

  behavior of "TimeIntervalReleaseRpsRule"

  it should "correctly apply percentage rule" in {
    //Given
    val percentageRule = new PercentageReleaseRpsRule(50)

    //When
    val actual = percentageRule.apply(10)

    //Then
    actual should be (5)
  }
}
