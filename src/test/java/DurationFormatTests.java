import net.hardwarelounge.gallium.command.PunishmentCommands;

import java.lang.reflect.Method;

public class DurationFormatTests {

    public static void main(String[] args) throws ReflectiveOperationException {
        Method parseDurationSeconds = PunishmentCommands.class
                .getDeclaredMethod("parseDurationSeconds", String.class);
        parseDurationSeconds.setAccessible(true);

        for (String duration : new String[]{"1s", "1m", "1h", "1d", "1M", "1y"}) {
            System.out.println(duration + " = "
                    + parseDurationSeconds.invoke(PunishmentCommands.class, duration) + "s");
        }
    }

}
