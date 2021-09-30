package net.hardwarelounge.gallium.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CommandSubconfig {
    private boolean isAllowedByDefault;
    private Map<String, Boolean> permissions;
}
