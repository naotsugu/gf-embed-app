package org.example.app;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.SequencedMap;
import java.util.logging.LogManager;
import java.util.stream.Stream;

import static java.lang.System.getLogger;
import static java.lang.System.Logger.*;

public class Main {

    private static final Logger log = getLogger(Main.class.getName());

    private static final String webApp = "web";
    private static final Path workDir = Path.of("gf");

    private final SequencedMap<String, String> props;
    private final GlassFish glassFish;

    public static void main(String[] args) {
        try {
            new Main().run();
        } catch (Exception e) {
            log.log(Level.ERROR, e.getMessage(), e);
        }
    }

    public Main() throws Exception {

        try (InputStream is = Main.class.getResourceAsStream("/logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread("GlassFish Shutdown Hook") {
            public void run() {
                stopGlassFish(Main.this.glassFish);
            }
        });

        if (Files.exists(workDir)) {
            try (Stream<Path> pathStream = Files.walk(workDir)) {
                pathStream.sorted(Comparator.reverseOrder()).forEach(Main::delete);
            }
        }

        props = readProperties();
        glassFish = GlassFishRuntime
            .bootstrap(new BootstrapProperties())
            .newGlassFish(glassFishProperties());
    }

    private void run() throws Exception {

        glassFish.start();
        setupGlassfish();

        try (InputStream is = Main.class.getResourceAsStream("/" + webApp + ".war")) {
            glassFish.getDeployer().deploy(is, "--name", webApp, "--contextroot", webApp, "--force", "true");
        }

        switch (glassFish.getStatus()) {
            case INIT, STARTING -> log.log(Level.INFO, "GlassFish is starting...");
            case STARTED        -> log.log(Level.INFO, "GlassFish is started");
            case STOPPING       -> log.log(Level.INFO, "GlassFish is shutting down...");
            default             -> log.log(Level.INFO, "GlassFish is shut down");
        }

        System.out.println("http://localhost:" + props.get("port") + "/" + webApp);

    }

    private void setupGlassfish() throws Exception {

        Path gfRoot = gfRoot();
        try (InputStream is = Main.class.getResourceAsStream("/docroot/404.html")) {
            Files.copy(is, gfRoot.resolve("docroot/404.html"), StandardCopyOption.REPLACE_EXISTING);
        }
        try (InputStream is = Main.class.getResourceAsStream("/docroot/robots.txt")) {
            Files.copy(is, gfRoot.resolve("docroot/robots.txt"), StandardCopyOption.REPLACE_EXISTING);
        }

        CommandRunner commandRunner = glassFish.getCommandRunner();
        var commands = props.entrySet().stream()
            .filter(e -> e.getKey().startsWith("command."))
            .map(Map.Entry::getValue)
            .map(line -> line.split("\\s", 2))
            .toList();
        for (var command : commands) {
            log.log(Level.INFO, "# command: {0}", Arrays.toString(command));
            var result = commandRunner.run(command[0], command[1].split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)"));
            switch (result.getExitStatus()) {
                case SUCCESS -> log.log(Level.INFO,    "# SUCCESS: {0}", result.getOutput());
                case WARNING -> log.log(Level.WARNING, "# WARNING: {0} {1}", result.getOutput(), result.getFailureCause());
                default      -> log.log(Level.ERROR,   "# FAILURE: {0}", result.getFailureCause());
            }
        }
    }

    private static SequencedMap<String, String> readProperties() {
        try (InputStream is = Main.class.getResourceAsStream("/glassfish.properties")) {
            SequencedMap<String, String> props = new LinkedHashMap<>();
            class Props extends Properties {
                @Override
                public synchronized Object put(Object key, Object value) {
                    return props.put((String) key, (String) value);
                }
            }
            new Props().load(is);
            return props;
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    private GlassFishProperties glassFishProperties() {
        var properties = new GlassFishProperties();
        properties.setPort("http-listener", Integer.parseInt(props.getOrDefault("port", "8080")));
        properties.setProperty("glassfish.embedded.tmpdir", workDir.toAbsolutePath().toString());
        return properties;
    }

    private Path gfRoot() {
        try (var paths = Files.list(workDir)) {
            return paths.filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().startsWith("gfembed"))
                .findFirst().orElseThrow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
