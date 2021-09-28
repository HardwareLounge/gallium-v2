package net.hardwarelounge.gallium.interaction;

import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.hardwarelounge.gallium.ticket.TicketType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TicketButtonListener extends ListenerAdapter {

    private static final String BUTTON_PREFIX = "ticket:";

    public List<Button> getButtons() {
        final List<Button> buttons = new ArrayList<>();

        for (TicketType type : TicketType.values()) {
            buttons.add(Button.of(
                    ButtonStyle.PRIMARY,
                    BUTTON_PREFIX + type.name(),
                    type.getName(),
                    Emoji.fromUnicode(type.getEmoji()))
            );
        }

        return buttons;
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (event.getComponentId().startsWith(BUTTON_PREFIX)) {
            try {
                TicketType.valueOf(TicketType.class, event.getComponentId().split(":")[1]);
            } catch (NullPointerException | IndexOutOfBoundsException exception) {
                event.reply("Invalid ticket type").setEphemeral(true).queue();
            }
        }
    }
}
