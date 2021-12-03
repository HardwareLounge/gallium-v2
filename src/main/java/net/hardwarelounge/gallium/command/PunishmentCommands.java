package net.hardwarelounge.gallium.command;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.ModAction;
import net.hardwarelounge.gallium.punishment.Punishment;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;

import javax.persistence.PersistenceException;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

public class PunishmentCommands {

    private static final Pattern DURATION_FORMAT_PATTERN = Pattern.compile("\\d+[smhdMy]");

    public abstract static class RolePunishmentCommand extends SlashCommand {
        private final @Getter Role role;
        public RolePunishmentCommand(DiscordBot parent, String uniqueName, String roleName) {
            super(parent, uniqueName);
            role = parent.getHome()
                    .getRoleById(parent.getPermissionConfig().getRoles().get(roleName).getDiscordRoleId());
        }
    }

    public static class WarnCommand extends RolePunishmentCommand {
        public WarnCommand(DiscordBot parent, String uniqueName, String roleName) {
            super(parent, uniqueName, roleName);
        }

        @Override
        public CommandData commandData() {
            return new CommandData("warn", "Warn a user")
                    .addOption(OptionType.USER, "target", "The user to warn", true)
                    .addOption(OptionType.STRING, "duration", "s:second, m:minute, h:hour, d:day, M:Month, y:year", true)
                    .addOption(OptionType.STRING, "cause", "Reason for the warn", true);
        }

        @Override
        public void execute(SlashCommandEvent event) {
            punish(parent, Punishment.WARN, event, getRole());
        }
    }

    public static class MuteCommand extends RolePunishmentCommand {
        public MuteCommand(DiscordBot parent, String uniqueName, String roleName) {
            super(parent, uniqueName, roleName);
        }

        @Override
        public CommandData commandData() {
            return new CommandData("mute", "Mute a user")
                    .addOption(OptionType.USER, "target", "The user to mute", true)
                    .addOption(OptionType.STRING, "duration", "s:second, m:minute, h:hour, d:day, M:Month, y:year", true)
                    .addOption(OptionType.STRING, "cause", "Reason for the mute", true);
        }

        @Override
        public void execute(SlashCommandEvent event) {
            punish(parent, Punishment.MUTE, event, getRole());
        }
    }

    public static class BanCommand  extends RolePunishmentCommand {
        public BanCommand(DiscordBot parent, String uniqueName, String roleName) {
            super(parent, uniqueName, roleName);
        }
        @Override
        public CommandData commandData() {
            return new CommandData("ban", "Role-ban a user")
                    .addOption(OptionType.USER, "target", "The user to ban", true)
                    .addOption(OptionType.STRING, "duration", "s:second, m:minute, h:hour, d:day, M:Month, y:year", true)
                    .addOption(OptionType.STRING, "cause", "Reason for the ban", true);
        }

        @Override
        public void execute(SlashCommandEvent event) {
            punish(parent, Punishment.ROLE_BAN, event, getRole());
        }
    }

    private static void punish(DiscordBot parent, Punishment type, SlashCommandEvent event, Role role) {
        String cause = Objects.requireNonNull(event.getOption("cause")).getAsString();
        User targetUser = Objects.requireNonNull(event.getOption("target")).getAsUser();
        CachedUser target = parent.getPunishmentManager().updateAndGetMember(targetUser);
        CachedUser moderator = parent.getPunishmentManager().updateAndGetMember(event.getMember());
        long duration = parseDurationSeconds(Objects.requireNonNull(event.getOption("duration")).getAsString());

        Member targetMember = parent.getHome().getMember(targetUser);
        if (targetMember == null) throw new CommandFailedException("Could not find this user");
        parent.getHome().addRoleToMember(targetMember, role).queue();

        ModAction result;

        try {
            result = parent.getPunishmentManager().punish(target, moderator, type, duration, cause);
        } catch (PersistenceException exception) {
            parent.getHome().removeRoleFromMember(targetMember, role).queue();
            throw exception;
        }

        event.replyEmbeds(EmbedUtil.defaultEmbed()
                .setTitle("Mod Action #" + result.getId())
                .setDescription(String.format("Aktion `%s` wurde auf user `%s(%s)` ausgeführt.",
                        type.name(), target.getId(), target.getNickname() + "#" + target.getDiscriminator()))
                .build()
        ).setEphemeral(true).queue();
    }

    private static long parseDurationSeconds(String fromString) {
        if (!DURATION_FORMAT_PATTERN.matcher(fromString).matches()) {
            throw new NumberFormatException();
        }

        long time = Long.parseLong(fromString.substring(0, fromString.length() - 1));
        return switch (fromString.charAt(fromString.length() - 1)) {
            case 's' -> Duration.ofSeconds(time).toSeconds();
            case 'm' -> Duration.ofMinutes(time).toSeconds();
            case 'h' -> Duration.ofHours(time).toSeconds();
            case 'd' -> Duration.ofDays(time).toSeconds();
            case 'M' -> Duration.ofDays(time * 30).toSeconds();
            case 'y' -> Duration.ofDays(time * 365).toSeconds();
            default -> throw new NumberFormatException();
        };
    }

}
