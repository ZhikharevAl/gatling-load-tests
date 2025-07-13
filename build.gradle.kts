plugins {
    kotlin("jvm") version "2.2.0"
    id("io.gatling.gradle") version "3.11.5.2"
}

group = "com.example.loadtest"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    gatling("io.gatling.highcharts:gatling-charts-highcharts:3.14.3")
    gatling("io.gatling:gatling-app:3.14.3")
    gatling("io.gatling:gatling-recorder:3.14.3")
    gatling("org.jetbrains.kotlin:kotlin-stdlib")
    gatling("org.jetbrains.kotlin:kotlin-reflect")
    gatling("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.1")
    gatling("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    gatling("org.apache.commons:commons-lang3:3.12.0")
    gatling("ch.qos.logback:logback-classic:1.5.18")
}

val simulations =
    listOf(
        "com.example.loadtest.BalanceLoadTest",
        "com.example.loadtest.HealthCheckTest",
        "com.example.loadtest.StressTest",
    )

simulations.forEach { simulation ->
    val taskName = "run${simulation.substringAfterLast(".")}"
    tasks.register<JavaExec>(taskName) {
        group = "gatling"
        description = "Run Gatling simulation: $simulation"
        mainClass.set("io.gatling.app.Gatling")
        classpath = sourceSets["gatling"].runtimeClasspath
        args =
            listOf(
                "-s",
                simulation,
                "-rf",
                "${project.layout.buildDirectory.get()}/reports/gatling",
            )
        jvmArgs =
            listOf(
                "--add-opens",
                "java.base/java.lang=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.util=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.util.concurrent=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.nio=ALL-UNNAMED",
                "--add-opens",
                "java.base/sun.nio.ch=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.net=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.io=ALL-UNNAMED",
                "--add-opens",
                "java.base/java.lang.reflect=ALL-UNNAMED",
            )
    }
}

gatling {
    jvmArgs =
        listOf(
            "--add-opens",
            "java.base/java.lang=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.lang.invoke=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.util=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.util.concurrent=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.nio=ALL-UNNAMED",
            "--add-opens",
            "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.net=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.io=ALL-UNNAMED",
            "--add-opens",
            "java.base/java.lang.reflect=ALL-UNNAMED",
        )
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.util.concurrent=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.nio=ALL-UNNAMED",
        "--add-opens",
        "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.net=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.io=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.lang.reflect=ALL-UNNAMED",
    )
}
