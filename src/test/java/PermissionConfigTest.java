import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.hardwarelounge.gallium.config.CommandSubconfig;
import net.hardwarelounge.gallium.config.LimitSubconfig;
import net.hardwarelounge.gallium.config.PermissionConfig;
import net.hardwarelounge.gallium.config.RoleSubconfig;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class PermissionConfigTest {

    public static void main(String[] args) throws IOException {
        PermissionConfig config = new PermissionConfig();
        config.setRoles(new HashMap<>());
        config.setCommands(new HashMap<>());
        config.setLimits(new HashMap<>());

        config.getRoles().put("hardware", new RoleSubconfig("Hardware Experte", "001"));
        config.getRoles().put("software", new RoleSubconfig("Software Experte", "002"));

        HashMap<String, Boolean> permissionMap = new HashMap<>();
        permissionMap.put("hardware", true);
        permissionMap.put("software", true);
        config.getCommands().put("ticket:*", new CommandSubconfig(false, permissionMap));

        HashMap<String, Integer> limitMap = new HashMap<>();
        limitMap.put("hardware", 30);
        limitMap.put("software", 30);
        config.getLimits().put("punishment_warn", new LimitSubconfig("Maximum Warn Time (in Days)", limitMap));

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.writeValue(new File("permissions.yml"), config);
    }

}
