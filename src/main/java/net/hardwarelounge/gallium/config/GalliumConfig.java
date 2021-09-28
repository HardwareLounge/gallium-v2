package net.hardwarelounge.gallium.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class GalliumConfig {

    private String token;
    private String homeGuildId;

    private String ticketChannelId;
    private String ticketCategoryId;
    private String ticketLogChannelId;

    private String database;
    private String databaseHost;
    private String databasePort;
    private String databaseUsername;
    private String databasePassword;
    private Boolean databaseUseSSL;

}
