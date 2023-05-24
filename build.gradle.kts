// See https://gradle.org and https://github.com/gradle/kotlin-dsl

// Apply the java plugin to add support for Java
plugins {
    java
    application
}

repositories {
    jcenter()
}

dependencies {
    // Annotations for better code documentation
    implementation("com.intellij:annotations:12.0")

    // JUnit test framework
    testImplementation("junit:junit:4.12")

    // HTTP client for unit tests
    testImplementation("org.apache.httpcomponents:fluent-hc:4.5.3")

    // Guava for tests
    testImplementation("com.google.guava:guava:23.1-jre")

    // https://mvnrepository.com/artifact/org.glassfish.grizzly/grizzly-http-server
    implementation("org.glassfish.grizzly:grizzly-http-server:4.0.0")
    implementation("org.glassfish.jersey.containers:jersey-container-grizzly2-http:3.1.0")
    implementation("org.glassfish.jersey.inject:jersey-hk2:3.1.0")
    implementation ("log4j:log4j:1.2.17")

    implementation("org.projectlombok:lombok:1.18.26")
    annotationProcessor("org.projectlombok:lombok:1.18.26")
}

tasks {
    "test"(Test::class) {
        maxHeapSize = "1g"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    // Define the main class for the application
    mainClassName = "ru.mail.polis.Cluster"

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx1g", "-Xverify:none")
}
