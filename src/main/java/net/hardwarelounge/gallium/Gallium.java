package net.hardwarelounge.gallium;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.hardwarelounge.gallium.config.GalliumConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class Gallium {

    public static void main(String[] args) throws Exception {
        Logger logger = LogManager.getLogger("Gallium");
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        File configFile = new File("config.yml");
        GalliumConfig config;

        try {
            config = objectMapper.readValue(configFile, GalliumConfig.class);
            objectMapper.writeValue(configFile, config);
        } catch (IOException exception) {
            config = new GalliumConfig();
            objectMapper.writeValue(configFile, config);
            logger.warn("Please configure file config.yml, the bot will stop now...");
            System.exit(78);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> logger.info("Shutting down Gallium...")));
        new DiscordBot(config).start();
    }

}
