plugins {
    id("buildlogic.java-common-conventions")
    war
}

dependencies {
    compileOnly("jakarta.platform:jakarta.jakartaee-api")
}
