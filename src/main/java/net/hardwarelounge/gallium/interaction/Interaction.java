package net.hardwarelounge.gallium.interaction;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.hardwarelounge.gallium.listener.SlashCommandListener;

public abstract class Interaction {

    private static long counter;

    private final String uniqueName;

    public Interaction(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public abstract void handleButtonInteraction(ButtonClickEvent event, String data);

    public abstract void handleSelectionMenu(SelectionMenuEvent event, String data);

    public final synchronized String createButtonId(String data) {
        return SlashCommandListener.COMMAND_DATA_PREFIX
                + SlashCommandListener.BUTTON_PREFIX + SlashCommandListener.INTERACTION_ID_DELIMITER
                + uniqueName + SlashCommandListener.INTERACTION_ID_DELIMITER + data
                + SlashCommandListener.INTERACTION_ID_DELIMITER + (++counter);
    }

    public final synchronized String createSelectMenuId(String data) {
        return SlashCommandListener.COMMAND_DATA_PREFIX
                + SlashCommandListener.SELECT_MENU_PREFIX + SlashCommandListener.INTERACTION_ID_DELIMITER
                + uniqueName + SlashCommandListener.INTERACTION_ID_DELIMITER + data
                + SlashCommandListener.INTERACTION_ID_DELIMITER + (++counter);
    }

}
