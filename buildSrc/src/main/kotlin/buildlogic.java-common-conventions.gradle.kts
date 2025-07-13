plugins {
    java
}

repositories {
    mavenCentral()
}

dependencies {
    constraints {
        implementation("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
