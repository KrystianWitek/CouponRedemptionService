import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

val projectVersion = "0.0.1-SNAPSHOT"
val javaVersion = 21

group = "com.krystianwitek"
version = projectVersion
description = "Coupon redemption service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

repositories {
    mavenCentral()
}

val integrationSourceSet =
    sourceSets.create("integration") {
        kotlin {
            compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
            runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
            srcDir("src/integration/kotlin")
        }
    }

configurations[integrationSourceSet.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[integrationSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(libs.kotlin.logging)
    implementation(libs.logbook.spring.boot.starter)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.wiremock.standalone)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    "integrationImplementation"("org.springframework.boot:spring-boot-starter-data-jpa-test")
    "integrationImplementation"(libs.spring.boot.testcontainers)
    "integrationImplementation"(libs.testcontainers.postgresql)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val integrationTest =
    tasks.register<Test>("integrationTest") {
        description = "Runs the integration tests."
        group = "verification"
        testClassesDirs = integrationSourceSet.output.classesDirs
        classpath = integrationSourceSet.runtimeClasspath
        shouldRunAfter(tasks.test)
    }

tasks.check {
    dependsOn(integrationTest)
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("application.jar")
}
