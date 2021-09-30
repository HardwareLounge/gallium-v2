package net.hardwarelounge.gallium.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PermissionConfig extends DefaultConfigFactory {

    private Map<String, RoleSubconfig> roles;
    private Map<String, CommandSubconfig> commands;
    private Map<String, LimitSubconfig> limits;

    public static PermissionConfig createDefault() {
        return new PermissionConfig(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }
}
