package net.hardwarelounge.gallium.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.ticket.TicketType;

import java.awt.Color;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class EmbedUtil {

    private static final String prefix = "https://hardwarelounge.github.io/gallium-v2/index.html?url=";

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

    public static EmbedBuilder ticketCloseEmbed(Ticket ticket, String url) {
        return defaultEmbed()
                .setTitle("Ticket #" + ticket.getId() + " by " + ticket.getOwner().getUsername()
                        + "#" + ticket.getOwner().getDiscriminator())
                .addField("ID", String.valueOf(ticket.getId()), true)
                .addField("Typ", ticket.getType().getName(), true)
                .addField("Name", ticket.getName(), true)
                .addField("Owner", ticket.getOwner().getAsMention() + " - "
                        + ticket.getOwner().getUsername() + "#" + ticket.getOwner().getDiscriminator(), true)
                .addField("Geschlossen durch", ticket.getClosedUser().getAsMention() + " - "
                        + ticket.getClosedUser().getUsername() + "#" + ticket.getClosedUser().getDiscriminator(), true)
                .addField("Schließungsgrund", ticket.getClosedCause(), true)
                .addField("Ticket-Log", prefix + URLEncoder.encode(url, StandardCharsets.UTF_8), false)
                ;
    }

}
