package net.hardwarelounge.gallium;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.SneakyThrows;
import net.hardwarelounge.gallium.config.DefaultConfigFactory;
import net.hardwarelounge.gallium.config.GalliumConfig;
import net.hardwarelounge.gallium.config.PermissionConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Gallium {

    private static Logger logger;
    private static boolean missingConfiguration;

    public static void main(String[] args) throws Exception {
        logger = LogManager.getLogger("Gallium");

        PermissionConfig permissions;
        GalliumConfig config;

        try {
            config = loadConfiguration(
                    new File("config.yml"),
                    GalliumConfig.class,
                    new YAMLFactory()
            );

            permissions = loadConfiguration(
                    new File("permissions.yml"),
                    PermissionConfig.class,
                    new YAMLFactory()
            );
        } catch (ReflectiveOperationException exception) {
            logger.error("Reflective Error while initializing configuration", exception);
            System.exit(78);
            return;
        }

        if (missingConfiguration) {
            System.exit(78);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.info("Shutting down Gallium...")));

        try {
            new DiscordBot(config, permissions).start();
        } catch (RuntimeException exception) {
            logger.error("Error while initializing bot", exception);
            System.exit(13);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends DefaultConfigFactory> T loadConfiguration(File configFile, Class<T> configClass, JsonFactory factory)
            throws IOException, ReflectiveOperationException {

        ObjectMapper objectMapper = new ObjectMapper(factory);
        T config;

        try {
            logger.info("Loading configuration {}, mapped by {}", configFile.getName(), configClass.getName());
            config = objectMapper.readValue(configFile, configClass);
            objectMapper.writeValue(configFile, config);
        } catch (FileNotFoundException exception) {
            logger.info("Creating empty configuration...");
            config = (T) configClass.getDeclaredMethod("createDefault").invoke(null);
            objectMapper.writeValue(configFile, config);
            logger.warn("Please configure file {}, " +
                    "the bot will stop after initializing all configs...", configFile.getName());
            missingConfiguration = true;
        } catch (IOException exception) {
            config = null;
            logger.warn("Please configure file {}, " +
                    "the bot will stop after initializing all configs...", configFile.getName());
            System.exit(78);
        }

        T defaultConfig = (T) configClass.getDeclaredMethod("createDefault").invoke(null);

        if (defaultConfig.equals(config)) {
            logger.warn("Config file {} is still the default config " +
                    "and might need to be configured!", configFile.getName());
        }

        return config;
    }

}
