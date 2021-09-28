package net.hardwarelounge.gallium.ticket;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.interaction.TicketButtonListener;
import net.hardwarelounge.gallium.util.EmbedUtil;

public class TicketManager {

    private final DiscordBot parent;
    private final TicketButtonListener ticketButtonListener;

    private final MessageChannel ticketChannel;
    private final MessageChannel ticketLogChannel;
    private final Category ticketCategory;

    public TicketManager(DiscordBot parent) {
        this.parent = parent;
        ticketButtonListener = new TicketButtonListener();

        ticketChannel = parent.getHome().getTextChannelById(parent.getConfig().getTicketChannelId());
        ticketLogChannel = parent.getHome().getTextChannelById(parent.getConfig().getTicketLogChannelId());
        ticketCategory = parent.getHome().getCategoryById(parent.getConfig().getTicketCategoryId());

        sendTicketMessage();
    }

    private void sendTicketMessage() {
        ticketChannel.getHistory().retrievePast(1).queue(messages -> {
            if (messages.isEmpty()) {
                MessageBuilder messageBuilder = new MessageBuilder(EmbedUtil.defaultEmbed());
                messageBuilder.setActionRows(ActionRow.of(ticketButtonListener.getButtons()));
                ticketChannel.sendMessage(messageBuilder.build()).queue();
            }
        });
    }

    public void openTicket() {
        parent.using(session -> {
            
        });
    }

}
