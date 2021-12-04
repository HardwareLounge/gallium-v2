package net.hardwarelounge.gallium.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.hardwarelounge.gallium.database.ModAction;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.ticket.TicketType;

import java.awt.Color;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class EmbedUtil {

    private static final String prefix = "https://hardwarelounge.github.io/gallium-v2/index.html?url=";

    public static EmbedBuilder defaultEmbed() {
        return new EmbedBuilder()
                .setColor(DiscordRoleColors.BLUE.getPrimary())
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
                .addField("Ticket-Log", prefix + URLEncoder.encode(url, StandardCharsets.UTF_8), false);
    }

    public static EmbedBuilder modActionEmbed(ModAction action) {
        return defaultEmbed()
                .setTitle("Mod Action #" + action.getId())
                .addField("ID", String.valueOf(action.getId()), true)
                .addField("Art", "`" + action.getType().name() + "`", true)
                .addField("Dauer", Duration.ofMillis(action.getDuration()).toString(), true)
                .addField("Moderator", action.getPerformedBy().toString(), false)
                .addField("Bestrafter", action.getPerformedOn().toString(), false)
                .addField("Grund", action.getPerformedBecause(), false)
                .addField("Erstellungszeitpunkt", String.valueOf(action.getPerformedAt()), true)
                .addField("Ist Aufgehoben?", String.valueOf(action.isPardoned()), true)
                .addField("Aufgh. von", String.valueOf(action.getPardonedBy()), true)
                .addField("Aufhebegrund", String.valueOf(action.getPardonedBecause()), true)
                .addField("Aufhebezeitpunkt", String.valueOf(action.getPardonedAt()), true);
    }

    public static EmbedBuilder logPunishment(ModAction action) {
        return defaultEmbed()
                .setTitle("Neue Bestrafung #" + action.getId())
                .setColor(DiscordRoleColors.RED.getPrimary())
                .addField("Art", "`" + action.getType().name() + "`", true)
                .addField("Dauer", Duration.ofMillis(action.getDuration()).toString(), true)
                .addField("Moderator", action.getPerformedBy().toString(), false)
                .addField("Bestrafter", action.getPerformedOn().toString(), false)
                .addField("Grund", action.getPerformedBecause(), false)
                .addField("Erstellungszeitpunkt", String.valueOf(action.getPerformedAt()), true);
    }

    public static EmbedBuilder logPardon(ModAction action) {
        return defaultEmbed()
                .setTitle("Bestrafung aufgehoben #" + action.getId())
                .setColor(DiscordRoleColors.GREEN.getPrimary())
                .addField("Art", "`" + action.getType().name() + "`", true)
                .addField("Dauer", Duration.ofMillis(action.getDuration()).toString(), true)
                .addField("Bestrafter", action.getPerformedOn().toString(), false)
                .addField("Grund", action.getPerformedBecause(), false)
                .addField("Aufgh. von", String.valueOf(action.getPardonedBy()), true)
                .addField("Aufhebegrund", String.valueOf(action.getPardonedBecause()), true)
                .addField("Aufhebezeitpunkt", String.valueOf(action.getPardonedAt()), true);
    }

    public static EmbedBuilder punishmentUserNotification(ModAction action) {
        return defaultEmbed()
                .setDescription(String.format("Ein Moderator hat dich mit einem `%s` für `%s` Sekunden bestraft!",
                        action.getType().name(), action.getDuration()))
                .setTitle("Mod Action #" + action.getId())
                .addField("ID", String.valueOf(action.getId()), true)
                .addField("Bestrafter", action.getPerformedOn().toString(), false)
                .addField("Grund", action.getPerformedBecause(), false)
                .addField("Erstellungszeitpunkt", String.valueOf(action.getPerformedAt()), true);
    }

}
