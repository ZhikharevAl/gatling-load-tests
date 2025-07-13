@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.example.loadtest

import com.example.loadtest.config.TestConfig
import io.gatling.javaapi.core.*
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.*
import io.gatling.javaapi.http.HttpDsl.*
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom

class BalanceLoadTest : Simulation() {
    // HTTP протокол конфигурация
    private val httpProtocol =
        http
            .baseUrl(TestConfig.BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader(TestConfig.CONTENT_TYPE)
            .userAgentHeader("Gatling Load Test")
            .check(status().`is`(200))

    private val testDataFeeder = csv("data/test-data.csv").random()

    private val dynamicDataFeeder =
        generateSequence {
            mapOf(
                "rqUID" to "gatling-${System.currentTimeMillis()}-${ThreadLocalRandom.current().nextInt(10000)}",
                "clientId" to generateClientId(),
                "account" to generateAccount(),
                "openDate" to "2023-01-01",
                "closeDate" to "2024-01-01",
            )
        }.iterator()

    // Сценарий: Базовая проверка баланса
    private val balanceScenario =
        scenario("Balance Calculation")
            .feed(dynamicDataFeeder)
            .exec(
                http("Post Balance Request")
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
                    .check(status().`is`(200))
                    .check(jsonPath("$.rqUID").exists())
                    .check(jsonPath("$.balance").exists())
                    .check(jsonPath("$.currency").exists())
                    .check(responseTimeInMillis().lt(TestConfig.RESPONSE_TIME_95P)),
            ).pause(
                Duration.ofSeconds(TestConfig.THINK_TIME_MIN.toLong()),
                Duration.ofSeconds(TestConfig.THINK_TIME_MAX.toLong()),
            )

    private val healthScenario =
        scenario("Health Check")
            .exec(
                http("Health Check")
                    .get(TestConfig.HEALTH_ENDPOINT)
                    .check(status().`is`(200))
                    .check(bodyString().`is`("OK")),
            ).pause(Duration.ofSeconds(5))

    private val mixedScenario =
        scenario("Mixed Load")
            .feed(dynamicDataFeeder)
            .exec(
                http("Health Check")
                    .get(TestConfig.HEALTH_ENDPOINT)
                    .check(status().`is`(200)),
            ).pause(1, 2)
            .exec(
                http("Balance Request")
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
                    .check(status().`is`(200))
                    .check(jsonPath("$.currency").saveAs("currency")),
            ).pause(1, 3)
            .exec(
                http("Actuator Health")
                    .get(TestConfig.ACTUATOR_HEALTH)
                    .check(status().`is`(200))
                    .check(jsonPath("$.status").`is`("UP")),
            )

    init {
        setUp(
            balanceScenario.injectOpen(
                rampUsers(TestConfig.BASE_USERS)
                    .during(Duration.ofSeconds(TestConfig.RAMP_UP_DURATION.toLong())),
                constantUsersPerSec(TestConfig.BASE_USERS.toDouble())
                    .during(Duration.ofSeconds(TestConfig.STEADY_STATE_DURATION.toLong())),
            ),
            healthScenario.injectOpen(
                constantUsersPerSec(2.0)
                    .during(Duration.ofSeconds(TestConfig.STEADY_STATE_DURATION.toLong())),
            ),
            mixedScenario.injectOpen(
                rampUsers(TestConfig.BASE_USERS / 2)
                    .during(Duration.ofSeconds(TestConfig.RAMP_UP_DURATION.toLong())),
                constantUsersPerSec((TestConfig.BASE_USERS / 2).toDouble())
                    .during(Duration.ofSeconds(TestConfig.STEADY_STATE_DURATION.toLong())),
            ),
        ).protocols(httpProtocol)
            .assertions(
                global().responseTime().percentile3().lt(TestConfig.RESPONSE_TIME_95P),
                global().responseTime().percentile4().lt(TestConfig.RESPONSE_TIME_99P),
                global().successfulRequests().percent().gt(TestConfig.SUCCESS_RATE),
                forAll().failedRequests().percent().lt(1.0),
            )
    }

    // Вспомогательные функции
    private fun generateClientId(): String {
        val prefixes = listOf("1", "8", "9") // RUB, US, EU
        val prefix = prefixes[ThreadLocalRandom.current().nextInt(prefixes.size)]
        val suffix = (1..18).map { ThreadLocalRandom.current().nextInt(10) }.joinToString("")
        return prefix + suffix
    }

    private fun generateAccount(): String = (1..20).map { ThreadLocalRandom.current().nextInt(10) }.joinToString("")
}
