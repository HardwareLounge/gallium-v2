package net.hardwarelounge.gallium.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GalliumConfig extends DefaultConfigFactory {

    private String token;
    private String homeGuildId;

    private String ticketChannelId;
    private String ticketCategoryId;
    private String ticketLogChannelId;
    private int maxTicketsPerUser;

    private String database;
    private String databaseHost;
    private String databasePort;
    private String databaseUsername;
    private String databasePassword;
    private Boolean databaseUseSSL;

    public static GalliumConfig createDefault() {
        GalliumConfig config = new GalliumConfig();
        config.setDatabaseUseSSL(true);
        return config;
    }
}
