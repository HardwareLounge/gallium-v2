package net.hardwarelounge.gallium.listener;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.command.InfoCommand;
import net.hardwarelounge.gallium.command.SlashCommand;
import net.hardwarelounge.gallium.command.TicketCommand;
import net.hardwarelounge.gallium.util.CommandFailedException;
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

    private final DiscordBot parent;
    private final Map<String, SlashCommand> commandMap;

    public SlashCommandListener(DiscordBot parent) {
        this.parent = parent;
        commandMap = new HashMap<>();

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

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (commandMap.containsKey(event.getName())) {
            try {
                commandMap.get(event.getName()).execute(event);
            } catch (NullPointerException | CommandFailedException exception) {
                if (event.isAcknowledged()) {
                    event.getHook().sendMessage(exception.getMessage().length() == 0 ? exception.getClass().getSimpleName() : exception.getMessage()).queue();
                } else {
                    event.reply(exception.getMessage()).queue();
                }
            }
        } else {
            event.reply("Fehler: der Befehl konnte nicht gefunden werden!").setEphemeral(true).queue();
        }
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
