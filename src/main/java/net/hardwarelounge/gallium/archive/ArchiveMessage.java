package net.hardwarelounge.gallium.archive;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveMessage {
    private String id;
    private String author;
    private String content;
    private String[] embeds;
    private String[] attachments;
}
