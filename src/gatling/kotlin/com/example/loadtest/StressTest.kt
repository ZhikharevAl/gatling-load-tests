@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.loadtest

import com.example.loadtest.config.TestConfig
import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.*
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class StressTest : Simulation() {
    private val httpProtocol =
        http
            .baseUrl(TestConfig.BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader(TestConfig.CONTENT_TYPE)
            .userAgentHeader("Gatling Stress Test")
            .check(status().not(500))

    private val stressDataFeeder =
        generateSequence {
            mapOf(
                "rqUID" to "stress-${System.currentTimeMillis()}-${ThreadLocalRandom.current().nextInt(100000)}",
                "clientId" to generateClientId(),
                "account" to generateAccount(),
                "openDate" to "2023-01-01",
                "closeDate" to "2024-01-01",
            )
        }.iterator()

    private val stressScenario =
        scenario("Stress Test")
            .feed(stressDataFeeder)
            .exec(
                http("Stress Balance Request")
                    .post(TestConfig.BALANCE_ENDPOINT)
                    .body(
                        StringBody(
                            """
                            {
                                "rqUID": "#{rqUID}",
                                "clientId": "#{clientId}",
                                "account": "#{account}",
                                "openDate": "#{openDate}",
                                "closeDate": "#{closeDate}"
                            }
                            """.trimIndent(),
                        ),
                    ).asJson()
                    .check(status().`in`(200, 429, 503)),
            ).pause(Duration.ofMillis(100), Duration.ofMillis(500)) // Минимальная пауза

    private val peakScenario =
        scenario("Peak Load")
            .feed(stressDataFeeder)
            .repeat(5, "repeatCounter")
            .on(
                exec(
                    http("Peak Request #{gatling.scenario.repeatCounter}")
                        .post(TestConfig.BALANCE_ENDPOINT)
                        .body(
                            StringBody(
                                """
                                {
                                    "rqUID": "#{rqUID}-#{gatling.scenario.repeatCounter}",
                                    "clientId": "#{clientId}",
                                    "account": "#{account}",
                                    "openDate": "#{openDate}",
                                    "closeDate": "#{closeDate}"
                                }
                                """.trimIndent(),
                            ),
                        ).asJson()
                        .check(status().`in`(200, 429, 503)),
                ).pause(Duration.ofMillis(50), Duration.ofMillis(200)),
            )

    init {
        setUp(
            stressScenario.injectOpen(
                nothingFor(Duration.ofSeconds(10)),
                rampUsers(TestConfig.STRESS_USERS).during(Duration.ofSeconds(30)),
                constantUsersPerSec(TestConfig.STRESS_USERS.toDouble()).during(Duration.ofSeconds(60)),
                rampUsers(TestConfig.PEAK_USERS).during(Duration.ofSeconds(30)),
                constantUsersPerSec(TestConfig.PEAK_USERS.toDouble()).during(Duration.ofSeconds(60)),
            ),
            peakScenario.injectOpen(
                nothingFor(Duration.ofSeconds(60)),
                atOnceUsers(TestConfig.PEAK_USERS),
                rampUsers(TestConfig.PEAK_USERS * 2).during(Duration.ofSeconds(30)),
            ),
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(5000), // Более мягкие требования
                global().responseTime().percentile4().lt(10000),
                global().successfulRequests().percent().gt(80.0), // Допускаем больше ошибок
                details("Stress Balance Request").responseTime().mean().lt(2000),
            )
    }

    private fun generateClientId(): String {
        val prefixes = listOf("1", "8", "9")
        val prefix = prefixes[ThreadLocalRandom.current().nextInt(prefixes.size)]
        val suffix = (1..18).map { ThreadLocalRandom.current().nextInt(10) }.joinToString("")
        return prefix + suffix
    }

    private fun generateAccount(): String = (1..20).map { ThreadLocalRandom.current().nextInt(10) }.joinToString("")
}
