package net.hardwarelounge.gallium.command;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.config.LimitSubconfig;
import net.hardwarelounge.gallium.config.PermissionConfig;
import net.hardwarelounge.gallium.config.RoleSubconfig;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.ModAction;
import net.hardwarelounge.gallium.punishment.Punishment;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.PersistenceException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Log4j2
public class PunishmentCommands {

    private static final Pattern DURATION_FORMAT_PATTERN = Pattern.compile("\\d+[smhdMy]");

    private static final Logger LOGGER = LogManager.getLogger(PunishmentCommands.class.getSimpleName());

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

        if (!checkLimit(parent.getPermissionConfig(), type, event.getMember(), duration)) {
            LOGGER.warn("{} exceeded limit (requested: {}s)", event.getMember(), duration);
            throw new CommandFailedException("Du hast keine Berechtigung dazu (Limit überschritten)");
        }

        ModAction result;

        try {
            parent.getHome().addRoleToMember(targetMember, role).queue();
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

        try {
            // notify the user
            targetMember.getUser().openPrivateChannel().queue(privateChannel -> privateChannel
                    .sendMessageEmbeds(EmbedUtil.punishmentUserNotification(result).build()).queue());
        } catch (ErrorResponseException exception) {
            log.info("Could not message user " + targetMember);
        }
    }

    private static boolean checkLimit(PermissionConfig config, Punishment type, Member member, long duration) {
        long maximumDuration = -1;

        if (!config.getLimits().containsKey("punishment_" + type.name().toLowerCase())) {
            LogManager.getLogger().error("punishment_" + type.name() + " doesn't exist", new RuntimeException());
        }

        LimitSubconfig limit = config.getLimits().get("punishment_" + type.name().toLowerCase());

        for (Role role : member.getRoles()) { // for each role from user
            for (Map.Entry<String, RoleSubconfig> entry : config.getRoles().entrySet()) { // and for each role in config
                if (role.getId().equals(entry.getValue().getDiscordRoleId()) // check if user role matches config role
                        && limit.getLimit().containsKey(entry.getKey())) { // and check if limit exists
                    long nextLimit = limit.getLimit().get(entry.getKey());
                    if (nextLimit > maximumDuration) {
                        maximumDuration = nextLimit;
                    }
                }
            }
        }

        if (maximumDuration == -1 && limit.isAllowedByDefault()) {
            return true;
        } else {
            return maximumDuration > 0 && duration <= maximumDuration;
        }
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
