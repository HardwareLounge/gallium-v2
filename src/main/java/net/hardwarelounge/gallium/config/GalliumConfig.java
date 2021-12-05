package net.hardwarelounge.gallium.config;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class GalliumConfig extends DefaultConfigFactory {

    private String token;
    private String homeGuildId;

    private String modLogChannelId;

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
        return GalliumConfig.builder()
                .token("Your Token Here")
                .homeGuildId("-1")
                .modLogChannelId("-1")
                .ticketCategoryId("-1")
                .ticketChannelId("-1")
                .ticketLogChannelId("-1")
                .maxTicketsPerUser(3)
                .database("gallium_v2")
                .databaseHost("mysql")
                .databasePort("3306")
                .databaseUsername("root")
                .databasePassword("root")
                .databaseUseSSL(true)
                .build();
    }
}
