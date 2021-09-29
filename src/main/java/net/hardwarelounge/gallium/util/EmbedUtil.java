package net.hardwarelounge.gallium.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
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
            builder.addField(type.getEmoji() + " " + type.getName(), type.getDescription(), false);
        }

        return builder;
    }

    public static EmbedBuilder ticketCreatedEmbed(User creator, TicketType type) {
        return defaultEmbed()
                .setTitle("DEIN " + type + " TICKET")
                .setDescription(String.format("""
                        Hallo %s! Um dein Ticket schnellstmöglich bearbeiten zu können, \
                        schildere deine Frage bitte so detailliert wie möglich. Ein \
                        Team-Mitglied wird sich in Kürze um dein Anliegen kümmern.
                        """, creator.getAsMention()))
                .addField("Ticket-Ersteller", creator.getAsTag(), false);
    }

}
