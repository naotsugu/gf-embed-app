plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation("org.glassfish.main.extras:glassfish-embedded-all:7.0.25")
    implementation(project(":web", "archives"))
}

tasks.named<Jar>("jar") {
    setProperty("zip64", true)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("war") }
    })
    from(sourceSets.main.get().output)
    manifest {
        attributes("Main-Class" to
            "org.example.app.Main")
        attributes("Add-Exports" to
            "java.naming/com.sun.jndi.ldap " +
            "java.base/jdk.internal.vm.annotation")
        attributes("Add-Opens" to
            "java.base/java.lang " +
            "java.base/java.io " +
            "java.base/java.util " +
            "java.base/sun.nio.fs " +
            "java.base/java.net " +
            "java.base/sun.net.www.protocol.jrt " +
            "java.base/sun.net.www.protocol.jar " +
            "java.base/jdk.internal.loader " +
            "java.naming/javax.naming.spi " +
            "java.rmi/sun.rmi.transport " +
            "jdk.management/com.sun.management.internal " +
            "java.base/jdk.internal.vm.annotation")
    }
}
