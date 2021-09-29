package net.hardwarelounge.gallium.command;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.ticket.TicketType;
import net.hardwarelounge.gallium.util.EmbedUtil;

import java.util.Objects;

public class TicketCommand extends SlashCommand {

    public TicketCommand(DiscordBot parent, String uniqueName) {
        super(parent, uniqueName);
    }

    @Override
    public CommandData commandData() {
        return new CommandData("ticket", "Verwalte das aktuelle Ticket").addSubcommands(
                new SubcommandData("rename", "Ändert den Ticket-Namen")
                        .addOption(OptionType.STRING, "name", "Neuer Ticket-Name", true),
                new SubcommandData("type", "Ändert den Ticket-Typ")
                        .addOption(OptionType.STRING, "type", "Mögliche Ticket-Typen: mod, gen, hw, sw, esp", true),
                new SubcommandData("add", "Fügt einen User zu dem aktuellen Ticket hinzu")
                        .addOption(OptionType.USER, "user", "Der hinzugefügte User", true),
                new SubcommandData("remove", "Entfernt einen User vom aktuellen Ticket")
                        .addOption(OptionType.USER, "user", "Der entfernte User", true),
                new SubcommandData("close", "Schließt das aktuelle Ticket")
                        .addOption(OptionType.STRING, "cause", "Der Grund der Schließung", false)
        );
    }

    @Override
    public void execute(SlashCommandEvent event) {
        switch (Objects.requireNonNull(event.getSubcommandName())) {
            case "rename" -> rename(event);
            case "type" -> type(event);
            case "add" -> add(event);
            case "remove" -> remove(event);
            case "close" -> close(event);
            default -> event.reply("Ungültiger Subcommand!").setEphemeral(true).queue();
        }
    }

    private void rename(SlashCommandEvent event) {
        String name = Objects.requireNonNull(event.getOption("name")).getAsString();
        // update database and get ticket
        Ticket ticket = parent.getTicketManager().renameTicketByChannel(event.getTextChannel(), name);
        // rename channel
        event.getTextChannel().getManager().setName(ticket.getType().getId() + "-" + ticket.getName()).queue();
        // notify user
        event.reply("")
                .addEmbeds(EmbedUtil.defaultEmbed().setDescription("Ticket in `"
                        + name.toLowerCase().replaceAll(" ", "-") + "` umbenannt!").build())
                .setEphemeral(false)
                .queue();
    }

    private void type(SlashCommandEvent event) {
        String typeId = Objects.requireNonNull(event.getOption("type")).getAsString();
        // find ticket type for id
        for (TicketType type : TicketType.values()) {
            if (type.getId().equalsIgnoreCase(typeId)) {
                // update database and get ticket
                Ticket ticket = parent.getTicketManager().changeTypeByChannel(event.getTextChannel(), type);
                // rename channel
                event.getTextChannel().getManager().setName(ticket.getType().getId() + "-" + ticket.getName()).queue();
                // notify user
                event.reply("")
                        .addEmbeds(EmbedUtil.defaultEmbed().setDescription("Ticket-Typ in `" + type.getName()
                                + "` geändert!").build())
                        .setEphemeral(false)
                        .queue();

                return;
            }
        }
        // ticket type not found
        event.reply(String.format("Der Ticket-Typ %s konnte nicht gefunden werden, "
                        + "bitte benutze: mod/gen/hw/sw/esp", typeId))
                .setEphemeral(true)
                .queue();
    }

    private void add(SlashCommandEvent event) {

    }

    private void remove(SlashCommandEvent event) {

    }

    private void close(SlashCommandEvent event) {

    }
}
