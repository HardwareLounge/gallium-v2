package net.hardwarelounge.gallium.interaction;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.hardwarelounge.gallium.ticket.TicketManager;
import net.hardwarelounge.gallium.ticket.TicketType;
import net.hardwarelounge.gallium.util.EmbedUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class TicketButtonListener extends InteractionListenerAdapter {

    private static final String BUTTON_PREFIX = "ticket:";

    private final TicketManager ticketManager;

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
    public void onButtonClickSafe(@NotNull ButtonClickEvent event) {
        if (event.getComponentId().startsWith(BUTTON_PREFIX)) {
            try {
                // get type from component
                TicketType type = TicketType.valueOf(TicketType.class, event.getComponentId().split(":")[1]);
                // create ticket db entry and channel
                TextChannel ticketChannel = ticketManager.openTicket(ticketManager
                        .updateAndGetMember(event.getMember()), type);
                ticketChannel.sendMessageEmbeds(EmbedUtil.ticketCreatedEmbed(event.getUser(), type).build()).queue();
                // update channel permissions
                ticketChannel.getManager().putPermissionOverride(Objects.requireNonNull(event.getMember()),
                        List.of(Permission.VIEW_CHANNEL),
                        List.of()
                ).queue();
                // notify user
                event.reply("\uD83C\uDF89 Dein Ticket wurde erstellt: " + ticketChannel.getAsMention())
                        .setEphemeral(true).queue();
            } catch (NullPointerException | IndexOutOfBoundsException exception) {
                exception.printStackTrace();
                event.reply("Invalid ticket type").setEphemeral(true).queue();
            }
        }
    }
}
