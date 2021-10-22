package net.hardwarelounge.gallium.archive;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode
public class ArchiveUserData {
    private String lastAvatar;
    private String lastNickname;
    private String lastUsername;
    private String lastTag;
    private String topRole;
    private int userType;
}
