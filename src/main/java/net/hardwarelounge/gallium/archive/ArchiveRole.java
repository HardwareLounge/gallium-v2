package net.hardwarelounge.gallium.archive;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveRole {
    private int color;
    private String name;
}
