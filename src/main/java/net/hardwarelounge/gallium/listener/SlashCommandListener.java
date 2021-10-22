package net.hardwarelounge.gallium.listener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.command.InfoCommand;
import net.hardwarelounge.gallium.command.SlashCommand;
import net.hardwarelounge.gallium.command.TicketCommand;
import net.hardwarelounge.gallium.config.CommandSubconfig;
import net.hardwarelounge.gallium.config.RoleSubconfig;
import net.hardwarelounge.gallium.util.CommandFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SlashCommandListener extends ListenerAdapter {

    /**
     * This String is used by the {@link SlashCommand#createButtonId(String)} and
     * {@link SlashCommand#createSelectMenuId(String)} to use a unique identifier per
     * {@link net.dv8tion.jda.api.interactions.components.Button} /
     * {@link net.dv8tion.jda.api.interactions.components.selections.SelectionMenu}
     */
    public static final String INTERACTION_ID_DELIMITER = "∆";
    public static final String COMMAND_DATA_PREFIX = "command-data";
    public static final String BUTTON_PREFIX = "button";
    public static final String SELECT_MENU_PREFIX = "select";

    private static final Logger commandLogger = LogManager.getLogger("SlashCommand");

    private final DiscordBot parent;
    private final Map<String, Role> roleMap;
    private final Map<String, SlashCommand> commandMap;

    public SlashCommandListener(DiscordBot parent) {
        this.parent = parent;
        commandMap = new HashMap<>();
        roleMap = new HashMap<>();

        loadRoles();

        // Register all slash-commands
        registerCommands(parent.getJda(),
                new InfoCommand(parent, "info"),
                new TicketCommand(parent, "ticket")
        );
    }

    private void registerCommands(JDA jda, SlashCommand... commands) {
        // put all commands into the map
        for (SlashCommand command : commands) {
            commandMap.put(command.commandData().getName(), command);
        }

        // register global commands
        jda.updateCommands().addCommands(
                commandMap.values().stream()
                        .filter(SlashCommand::isGlobal)
                        .map(SlashCommand::commandData)
                        .collect(Collectors.toList())
        ).queue();

        // register all guild commands
        for (Guild guild : jda.getGuilds()) {
            guild.updateCommands().addCommands(
                    commandMap.values().stream()
                            .filter(command -> !command.isGlobal())
                            .map(SlashCommand::commandData)
                            .collect(Collectors.toList())
            ).queue();
        }
    }

    private void loadRoles() {
        for (Map.Entry<String, RoleSubconfig> roleEntry : parent.getPermissionConfig().getRoles().entrySet()) {
            Role role = parent.getHome().getRoleById(roleEntry.getValue().getDiscordRoleId());

            if (role == null) {
                throw new IllegalStateException(String.format("Role %s <#%s> does not exist inside home guild!",
                        roleEntry.getKey(), roleEntry.getValue().getDiscordRoleId()));
            }

            roleMap.put(roleEntry.getKey(), role);
        }
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (commandMap.containsKey(event.getName())) {
            try {
                // the member is null if the slash command was executed per direct message
                if (event.getMember() == null
                        || hasPermission(event.getName(), event.getSubcommandName(), event.getMember())) {
                    // log message
                    commandLogger.info("{} executed {}",
                            event.getUser(), commandToString(event));
                    // execute command
                    commandMap.get(event.getName()).execute(event);
                } else {
                    // log message
                    commandLogger.warn("{} has no permission to execute {}",
                            event.getUser(), commandToString(event));
                    // notify user
                    if (event.isAcknowledged()) {
                        event.getHook().sendMessage("Du hast keine Berechtigung " +
                                "diesen Befehl auszuführen!").queue();
                    } else {
                        event.reply("Du hast keine Berechtigung " +
                                "diesen Befehl auszuführen!").queue();
                    }
                }
            } catch (NullPointerException | CommandFailedException exception) {
                // notify user about failure
                if (event.isAcknowledged()) {
                    event.getHook().sendMessage(exception.getMessage().length() == 0 ?
                            exception.getClass().getSimpleName() : exception.getMessage()).queue();
                } else {
                    event.reply(exception.getMessage()).queue();
                }
            }
        } else {
            event.reply("Fehler: der Befehl konnte nicht gefunden werden!").setEphemeral(true).queue();
        }
    }

    private String commandToString(SlashCommandEvent event) {
        StringBuilder command = new StringBuilder("/" + event.getName());
        command.append(event.getSubcommandGroup() != null && !event.getSubcommandGroup().isBlank() ?
                " " + event.getSubcommandGroup() : "");
        command.append(event.getSubcommandName() != null && !event.getSubcommandName().isBlank() ?
                " " + event.getSubcommandName() : "");

        for (OptionMapping option : event.getOptions()) {
            command.append(" ").append(option.toString());
        }

        return command.toString();
    }

    private boolean hasPermission(final String command, final String subcommand, Member member) {
        if (!member.getGuild().equals(parent.getHome())) {
            // return false if the command was executed in a different guild
            return false;
        } else if (member.getPermissions().contains(Permission.ADMINISTRATOR)) {
            return true;
        }

        // find CommandSubconfig permission object from the permissions configuration
        CommandSubconfig permissions = parent.getPermissionConfig()
                .getCommands()
                .entrySet()
                .stream()
                // filter for entries matching command:* or command:subcommand
                .filter(entry -> {
                    String[] split = entry.getKey().split(":");
                    if (split.length == 2) {
                        return split[0].equalsIgnoreCase(command)
                                && (split[1].equalsIgnoreCase(subcommand) || split[1].equals("*"));
                    } else {
                        return false;
                    }
                })
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (permissions == null || permissions.getPermissions() == null) {
            return false;
        }

        if (permissions.getPermissions().containsKey("*")) {
            return permissions.getPermissions().get("*");
        }

        // check if any of the members roles are set to be allowed for the current command
        return permissions.isAllowedByDefault() || member.getRoles()
                .stream()
                // filter by roles the member has
                .filter(roleMap::containsValue)
                // map the JDA Role objects to roleNames from the permissions config
                .map(role -> roleMap.entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().equals(role))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(null)
                )
                // check if any of the roleNames are set to be allowed for the command
                .anyMatch(roleName -> permissions.getPermissions().containsKey(roleName)
                                && permissions.getPermissions().get(roleName));
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().startsWith(COMMAND_DATA_PREFIX)) {
            return;
        }

        String componentId = event.getComponentId().replaceFirst(COMMAND_DATA_PREFIX, "");

        String[] split = componentId.split(INTERACTION_ID_DELIMITER);

        if (split.length != 4 || !split[0].equalsIgnoreCase(SELECT_MENU_PREFIX)) {
            event.reply("Ein interner Fehler ist aufgetreten").setEphemeral(true).queue();
        } else if (commandMap.containsKey(split[1])) {
            try {
                commandMap.get(split[1]).handleSelectionMenu(event, split[2]);
            } catch (NullPointerException | CommandFailedException exception) {
                if (event.isAcknowledged()) {
                    event.getHook().sendMessage(exception.getMessage()).queue();
                } else {
                    event.reply(exception.getMessage()).queue();
                }
            }
        } else {
            event.reply("Ein interner Fehler ist aufgetreten").setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getComponentId().startsWith(COMMAND_DATA_PREFIX)) {
            return;
        }

        String componentId = event.getComponentId().replaceFirst(COMMAND_DATA_PREFIX, "");

        String[] split = componentId.split(INTERACTION_ID_DELIMITER);

        if (split.length != 4 || !split[0].equalsIgnoreCase(BUTTON_PREFIX)) {
            event.reply("Ein interner Fehler ist aufgetreten").setEphemeral(true).queue();
        } else if (commandMap.containsKey(split[1])) {
            try {
                commandMap.get(split[1]).handleButtonInteraction(event, split[2]);
            } catch (NullPointerException | CommandFailedException exception) {
                if (event.isAcknowledged()) {
                    event.getHook().sendMessage(exception.getMessage()).queue();
                } else {
                    event.reply(exception.getMessage()).queue();
                }
            }
        } else {
            event.reply("Ein interner Fehler ist aufgetreten").setEphemeral(true).queue();
        }
    }

    /**
     * Adds the commands to a guild, when new guild is joined.
     *
     * @param event the event
     */
    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        for (SlashCommand command : commandMap.values()) {
            if (!command.isGlobal()) {
                event.getGuild().upsertCommand(command.commandData()).queue();
            }
        }
    }

    public Map<String, SlashCommand> getCommandMap() {
        return commandMap;
    }

}
