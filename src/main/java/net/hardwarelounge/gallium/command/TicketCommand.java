package net.hardwarelounge.gallium.command;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.AttachmentOption;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.archive.DiscordChannelArchive;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.ticket.TicketType;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        Member member = Objects.requireNonNull(event.getOption("user")).getAsMember();

        if (member == null) {
            throw new CommandFailedException("Bitte gib ein gültiges Mitglied dieses Servers an!");
        }

        parent.getTicketManager().addUser(event.getTextChannel(), parent.getTicketManager().updateAndGetMember(member));

        event.getTextChannel().getManager()
                .putPermissionOverride(member, List.of(Permission.VIEW_CHANNEL), List.of())
                .queue();

        event.replyEmbeds(EmbedUtil.defaultEmbed()
                .setDescription(member.getAsMention() + " wurde von "
                        + event.getUser().getAsMention() + " zum Ticket hinzugefügt").build())
                .setEphemeral(false)
                .queue();
    }

    private void remove(SlashCommandEvent event) {
        Member member = Objects.requireNonNull(event.getOption("user")).getAsMember();

        if (member == null) {
            throw new CommandFailedException("Bitte gib ein gültiges Mitglied dieses Servers an!");
        }

        parent.getTicketManager().removeUser(event.getTextChannel(), parent.getTicketManager().updateAndGetMember(member));

        event.getTextChannel().getManager()
                .putPermissionOverride(member, List.of(), List.of(Permission.VIEW_CHANNEL))
                .queue();

        event.replyEmbeds(EmbedUtil.defaultEmbed()
                        .setDescription(member.getAsMention() + " wurde von "
                                + event.getUser().getAsMention() + " vom Ticket entfernt").build())
                .setEphemeral(false)
                .queue();
    }

    private void close(SlashCommandEvent event) {
        String cause = Optional.ofNullable(event.getOption("cause"))
                .map(OptionMapping::getAsString)
                .orElse("No cause provided");

        event.getTextChannel().sendTyping().queue();
        Ticket ticket = parent.getTicketManager().closeTicket(
                event.getTextChannel(),
                parent.getTicketManager().updateAndGetMember(event.getMember()),
                cause
        );

        // TODO: "correctly" implement channel archive (see net.hardwarelounge.gallium.archive.*)
        DiscordChannelArchive archive = parent.getTicketManager().archiveTicket(ticket);

        try {
            ObjectMapper objectMapper = new ObjectMapper(new JsonFactory());
            InputStream fileStream = new ByteArrayInputStream(objectMapper
                    .writeValueAsString(archive).getBytes(StandardCharsets.UTF_8));

            parent.getTicketManager().getTicketLogChannel()
                    .sendFile(fileStream, ticket.getId() + "-" + ticket.getName() + ".ticket")
                    .queue(message -> {
                        List<Message.Attachment> attachments = message.getAttachments();
                        if (attachments.isEmpty()) {
                            message.editMessage("Failed uploading ticket!").queue();
                        } else {
                            message.editMessage(
                                    new MessageBuilder()
                                            .setEmbeds(EmbedUtil.ticketCloseEmbed(ticket, attachments.get(0).getUrl()).build())
                                            .build()
                            ).queue();
                        }
                    });
        } catch (JsonProcessingException exception) {
            parent.getLogger().error(exception);
            throw new CommandFailedException("Failed to serialize ticket object");
        }

        Objects.requireNonNull(parent.getHome().getTextChannelById(ticket.getDiscordChannelId())).delete().queue();
    }
}
