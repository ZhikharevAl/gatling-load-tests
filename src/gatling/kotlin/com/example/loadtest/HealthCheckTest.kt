@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.loadtest

import com.example.loadtest.config.TestConfig
import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.*
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration

class HealthCheckTest : Simulation() {
    private val httpProtocol =
        http
            .baseUrl(TestConfig.BASE_URL)
            .acceptHeader("application/json, text/plain")
            .userAgentHeader("Gatling Health Check")

    private val healthScenario =
        scenario("Health Check")
            .exec(
                http("App Health")
                    .get(TestConfig.HEALTH_ENDPOINT)
                    .check(status().`is`(200))
                    .check(bodyString().`is`("OK")),
            ).pause(Duration.ofSeconds(1))

    private val actuatorScenario =
        scenario("Actuator Health")
            .exec(
                http("Actuator Health")
                    .get(TestConfig.ACTUATOR_HEALTH)
                    .check(status().`is`(200))
                    .check(jsonPath("$.status").`is`("UP")),
            ).pause(Duration.ofSeconds(2))

    private val metricsScenario =
        scenario("Metrics Check")
            .exec(
                http("Prometheus Metrics")
                    .get(TestConfig.METRICS_ENDPOINT)
                    .check(status().`is`(200))
                    .check(regex("jvm_memory_used_bytes").exists())
                    .check(regex("balance_requests_total").exists()),
            ).pause(Duration.ofSeconds(5))

    init {
        setUp(
            healthScenario.injectOpen(
                constantUsersPerSec(1.0).during(Duration.ofSeconds(60)),
            ),
            actuatorScenario.injectOpen(
                constantUsersPerSec(0.5).during(Duration.ofSeconds(60)),
            ),
            metricsScenario.injectOpen(
                constantUsersPerSec(0.2).during(Duration.ofSeconds(60)),
            ),
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(500),
                global().responseTime().max().lt(1000),
                global().successfulRequests().percent().`is`(100.0),
            )
    }
}
