package net.hardwarelounge.gallium.archive;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @version discord-channel-archive Version 1.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode
public class DiscordChannelArchive {
    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private String id;

    @JsonProperty("topic")
    private String topic;

    @JsonProperty("roles")
    private Map<String, ArchiveRole> roles;

    @JsonProperty("userdata")
    private Map<String, ArchiveUserData> userData;

    @JsonProperty("messages")
    private ArchiveMessage[] messages;
}
