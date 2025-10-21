plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation("org.glassfish.main.extras:glassfish-embedded-all:8.0.0-M13")
    implementation(project(":web", "archives"))

    //runtimeOnly("org.slf4j:slf4j-jdk14:2.0.17")
    runtimeOnly("org.slf4j:jul-to-slf4j:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
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

tasks.register<Exec>("jpackage") {

    dependsOn(tasks.jar)

    val javaToolchainService = project.extensions.getByType(JavaToolchainService::class.java)
    val jdkPath = javaToolchainService.launcherFor(java.toolchain).get().executablePath
    println("Toolchain JDK Path: $jdkPath")

    val commandPath = File(jdkPath.asFile.parentFile, "jpackage").absolutePath
    val outputDir = project.layout.buildDirectory.dir("jpackage")
    val inputDir = tasks.jar.get().archiveFile.get().asFile.parentFile
    val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()

    val commands = mutableListOf(
        commandPath,
        "--type", "app-image",
        "--name", "app",
        "--dest", outputDir.get().asFile.absolutePath,
        "--input", inputDir.absolutePath,
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "org.example.app.Main",

        "--java-options", "--add-opens java.base/java.lang=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/java.io=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/java.util=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/sun.nio.fs=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/java.net=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/sun.net.www.protocol.jrt=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/sun.net.www.protocol.jar=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED",
        "--java-options", "--add-opens java.naming/javax.naming.spi=ALL-UNNAMED",
        "--java-options", "--add-opens java.rmi/sun.rmi.transport=ALL-UNNAMED",
        "--java-options", "--add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED",
        "--java-options", "--add-opens java.base/jdk.internal.vm.annotation=ALL-UNNAMED",
    )
    if (os.isWindows) commands.add("--win-console")

    commandLine(commands)

    doFirst {
        if (outputDir.get().asFile.exists()) {
            outputDir.get().asFile.deleteRecursively()
        }
    }
}
