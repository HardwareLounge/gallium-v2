package net.hardwarelounge.gallium.archive;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ArchiveMessage {
    private String id;
    private String author;
    private String content;
    private String[] embeds;
    private String[] attachments;
}
