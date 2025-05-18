plugins {
    kotlin("jvm") version "2.0.20"
    `maven-publish`
}

group = "com.tiefensuche"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.json:json:20250107")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}