package net.hardwarelounge.gallium.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.hardwarelounge.gallium.ticket.TicketType;

import java.awt.*;
import java.time.Instant;

public class EmbedUtil {

    public static EmbedBuilder defaultEmbed() {
        return new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTimestamp(Instant.now())
                .setFooter("HLGuard by HardwareLounge.de");
    }

    public static EmbedBuilder ticketMessageEmbed() {
        EmbedBuilder builder = defaultEmbed()
                .setTitle("Neues Ticket Erstellen")
                .setDescription("Reagiere auf diese Nachricht um ein neues Ticket zu erstellen\n");

        for (TicketType type : TicketType.values()) {
            builder.addField(String.format(":%s: %s", type.getEmoji(), type.getName()),
                    type.getDescription(), false);
        }

        return builder;
    }

}
