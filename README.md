# gf-embed-app

A sample for bundling a web application with the GlassFish server.

## Generate the fat jar(uber jar) bundled with Jakarta EE runtime

```shell
./gradlew build
java -jar app/build/libs/app.jar
```

## Generate an application image bundled with the Java runtime

```shell
./gradlew jpackage
```

