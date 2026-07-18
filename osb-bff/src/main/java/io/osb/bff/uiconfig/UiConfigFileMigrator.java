package io.osb.bff.uiconfig;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * One-time import of legacy per-user JSON files into Postgres when the DB row is still missing.
 */
@ApplicationScoped
public class UiConfigFileMigrator {

    private static final Logger LOG = Logger.getLogger(UiConfigFileMigrator.class);

    private final UiConfigStore store;
    private final Path baseDir;

    @Inject
    public UiConfigFileMigrator(
            UiConfigStore store,
            @ConfigProperty(name = "osb.bff.ui-config-dir", defaultValue = "data/ui-config")
                    String baseDir) {
        this.store = store;
        this.baseDir = Path.of(baseDir);
    }

    void onStart(@Observes StartupEvent event) {
        if (!Files.isDirectory(baseDir)) {
            return;
        }
        HexFormat hex = HexFormat.of();
        int imported = 0;
        try (Stream<Path> users = Files.list(baseDir)) {
            for (Path userDir : users.filter(Files::isDirectory).toList()) {
                String userName = decodeHexName(hex, userDir.getFileName().toString());
                if (userName == null) {
                    continue;
                }
                try (Stream<Path> files = Files.list(userDir)) {
                    for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .toList()) {
                        String fileName = file.getFileName().toString();
                        String keyHex = fileName.substring(0, fileName.length() - ".json".length());
                        String key = decodeHexName(hex, keyHex);
                        if (key == null) {
                            continue;
                        }
                        if (store.find(userName, key).isPresent()) {
                            continue;
                        }
                        String json = Files.readString(file, StandardCharsets.UTF_8);
                        store.save(userName, key, json);
                        imported++;
                        LOG.infof("Imported UI config file into Postgres: user=%s key=%s", userName, key);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warnf(e, "Skipping legacy UI config file migration from %s", baseDir.toAbsolutePath());
            return;
        }
        if (imported > 0) {
            LOG.infof("Imported %d legacy UI config document(s) from %s", imported, baseDir);
        }
    }

    private static String decodeHexName(HexFormat hex, String value) {
        try {
            return new String(hex.parseHex(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
