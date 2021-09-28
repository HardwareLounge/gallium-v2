package net.hardwarelounge.gallium.command;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.interaction.Interaction;
import org.jetbrains.annotations.Nullable;

/**
 * Abstraction-layer to easier handle slash commands
 *
 * @author Christian Schliz (code@foxat.de)
 */
public abstract class SlashCommand extends Interaction {

    protected final DiscordBot parent;

    public SlashCommand(DiscordBot parent, String uniqueName) {
        super(uniqueName);
        this.parent = parent;
    }

    /**
     * JDA Command Data information used to register
     * and search commands.
     *
     * @return command data
     */
    public abstract CommandData commandData();

    /**
     * Defines whether the command should be registered
     * globally or per guild only.
     *
     * @return whether the command is global
     */
    public boolean isGlobal() {
        return false;
    }

    /**
     * Execute is called by the onSlashCommand event when
     * this command should be executed
     *
     * @param event original event
     */
    public abstract void execute(SlashCommandEvent event);

    @Override
    public void handleButtonInteraction(ButtonClickEvent event, String data) {
        // do nothing on default since not every slash command has button interactions
    }

    @Override
    public void handleSelectionMenu(SelectionMenuEvent event, String data) {
        // do nothing on default since not every slash command has menu interactions
    }

    public final String getName() {
        return this.commandData().getName();
    }

    /**
     * Returns a string to be displayed in the help-command
     * @return the help-string
     */
    @Nullable
    public String getHelpDescription() {
        return null;
    }

}
