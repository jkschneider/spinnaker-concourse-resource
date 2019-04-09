package io.pivotal.spinnaker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CheckTest {
    @Test
    fun checkLocalSpinnaker() {
        val stages = SpinnakerConcourseResource.check(Request(
                Source(
                        "http://localhost:8084",
                        "metricsdemo",
                        "multifoundationmetrics",
                        "spinnaker-stage-exec"),
                Version("1")
        ))

        assertThat(stages).isNotEmpty
    }
}