package com.example.loadtest.config

object TestConfig {
    const val BASE_URL = "http://localhost:8080"
    const val CONTENT_TYPE = "application/json"

    // Эндпоинты
    const val BALANCE_ENDPOINT = "/info/postBalances"
    const val HEALTH_ENDPOINT = "/info/health"
    const val ACTUATOR_HEALTH = "/actuator/health"
    const val METRICS_ENDPOINT = "/actuator/prometheus"

    // Настройки нагрузки
    const val RAMP_UP_DURATION = 30
    const val STEADY_STATE_DURATION = 120
    const val RAMP_DOWN_DURATION = 30

    // Пользователи
    const val BASE_USERS = 10
    const val STRESS_USERS = 50
    const val PEAK_USERS = 100

    // Тайминги
    const val THINK_TIME_MIN = 1 // секунда
    const val THINK_TIME_MAX = 3 // секунды
    const val REQUEST_TIMEOUT = 30 // секунд

    // Процентили для SLA
    const val RESPONSE_TIME_95P = 1000 // мс
    const val RESPONSE_TIME_99P = 2000 // мс
    const val SUCCESS_RATE = 95.0 // процент
}
