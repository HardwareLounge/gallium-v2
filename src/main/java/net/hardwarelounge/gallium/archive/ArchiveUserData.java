package net.hardwarelounge.gallium.archive;

import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveUserData {
    private String lastAvatar;
    private String lastNickname;
    private String lastUsername;
    private String lastTag;
    private String topRole;
    private int userType;
}
