package net.hardwarelounge.gallium.interaction;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.hardwarelounge.gallium.util.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import javax.persistence.PersistenceException;

public class InteractionListenerAdapter extends ListenerAdapter {

    @Override
    public final void onButtonClick(@NotNull ButtonClickEvent event) {
        try {
            onButtonClickSafe(event);
        } catch (CommandFailedException exception) {
            sendErrorMessage(event, exception.getMessage());
        } catch (PersistenceException exception) {
            exception.printStackTrace();
            sendErrorMessage(event, "Ein Datenbankfehler ist aufgetreten (Code: 101)");
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            sendErrorMessage(event, "Ein unbekannter Fehler ist aufgetreten (Code: 100)");
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        try {
            onSelectionMenuSafe(event);
        } catch (CommandFailedException exception) {
            exception.printStackTrace();
            sendErrorMessage(event, exception.getMessage());
        } catch (PersistenceException exception) {
            exception.printStackTrace();
            sendErrorMessage(event, "Ein Datenbankfehler ist aufgetreten (Code: 101)");
        } catch (RuntimeException exception) {
            exception.printStackTrace();
            sendErrorMessage(event, "Ein unbekannter Fehler ist aufgetreten (Code: 100)");
        }
    }

    private void sendErrorMessage(Interaction interaction, String message) {
        if (interaction.isAcknowledged()) {
            interaction.getHook().setEphemeral(true).sendMessage(message).queue();
        } else {
            interaction.reply(message).setEphemeral(true).queue();
        }
    }

    @SuppressWarnings("unused")
    public void onButtonClickSafe(@NotNull ButtonClickEvent event) {
    }

    @SuppressWarnings("unused")
    public void onSelectionMenuSafe(@NotNull SelectionMenuEvent event) {
    }

}
