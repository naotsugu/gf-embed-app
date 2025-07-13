package org.example.app;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.System.getLogger;
import static java.lang.System.Logger.*;

public class Main {

    private static final Logger log = getLogger(Main.class.getName());

    private static final int port = 8080;
    private static final Path workPath = Path.of("gf");
    private final GlassFish glassFish;

    public static void main(String[] args) {
        try {
            new Main().run();
        } catch (Exception e) {
            log.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public Main() throws GlassFishException {

        Runtime.getRuntime().addShutdownHook(new Thread("GlassFish Shutdown Hook") {
            public void run() {
                stopGlassFish(Main.this.glassFish);
            }
        });

        this.glassFish = GlassFishRuntime
            .bootstrap(new BootstrapProperties())
            .newGlassFish(glassFishProperties());

    }

    private void run() throws Exception {

        glassFish.start();

        try (InputStream is = Main.class.getResourceAsStream("/web.war")) {
            glassFish.getDeployer().deploy(is, "--name web --contextroot tricolor --force true".split("\\s"));
        }

        switch (glassFish.getStatus()) {
            case INIT, STARTING -> log.log(Level.INFO, "GlassFish is starting...");
            case STARTED        -> log.log(Level.INFO, "GlassFish is started");
            case STOPPING       -> log.log(Level.INFO, "GlassFish is shutting down...");
            default             -> log.log(Level.INFO, "GlassFish is shut down");
        }
    }

    private static void stopGlassFish(GlassFish gf) {
        if (gf == null) return;
        try {
            log.log(Level.INFO, "GlassFish is stopping...");
            gf.stop();
        } catch (GlassFishException e) {
            log.log(Level.ERROR, e.getMessage(), e);
        } finally {
            try {
                gf.dispose();
            } catch (GlassFishException e) {
                log.log(Level.ERROR, e.getMessage(), e);
            }
        }
    }

    private static GlassFishProperties glassFishProperties() {
        var properties = new GlassFishProperties();
        properties.setPort("http-listener", port);
        properties.setProperty("glassfish.embedded.tmpdir", workPath.toAbsolutePath().toString());
        return properties;
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
