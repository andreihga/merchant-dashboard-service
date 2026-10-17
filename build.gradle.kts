import org.gradle.testing.jacoco.tasks.JacocoReportBase

plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.26.0"
    id("com.diffplug.spotless") version "8.10.4"
}

group = "com.awin"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val openApiOutput = layout.buildDirectory.dir("generated/openapi")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-micrometer-tracing-brave")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.instancio:instancio-junit:6.1.0")
    testRuntimeOnly("com.h2database:h2")
}

sourceSets.named("main") {
    java.srcDir(openApiOutput.map { it.dir("src/main/java") })
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(
        layout.projectDirectory
            .file("src/main/resources/openapi/openapi.yaml")
            .asFile.absolutePath,
    )
    outputDir.set(openApiOutput.get().asFile.absolutePath)
    apiPackage.set("com.awin.merchantdashboard.api")
    modelPackage.set("com.awin.merchantdashboard.api.model")
    invokerPackage.set("com.awin.merchantdashboard.api")
    configOptions.set(
        mapOf(
            "delegatePattern" to "true",
            "useSpringBoot4" to "true",
            "useSpringBoot3" to "false",
            "useJackson3" to "true",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "interfaceOnly" to "false",
            "openApiNullable" to "false",
            "hideGenerationTimestamp" to "true",
            "useTags" to "true",
            "dateLibrary" to "java8",
        ),
    )
    globalProperties.set(mapOf("supportingFiles" to "false"))
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

val generatedApiClasses = listOf("com/awin/merchantdashboard/api/**")

tasks.withType<JacocoReportBase>().configureEach {
    classDirectories.setFrom(
        sourceSets.named("main").get().output.asFileTree.matching {
            exclude(generatedApiClasses)
        },
    )
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.102.0")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
