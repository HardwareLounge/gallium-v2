package net.hardwarelounge.gallium.ticket;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.database.TicketMessage;
import net.hardwarelounge.gallium.interaction.TicketButtonListener;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TicketManager {

    private final DiscordBot parent;
    private final TicketButtonListener ticketButtonListener;

    private final MessageChannel ticketChannel;
    private final @Getter MessageChannel ticketLogChannel;
    private final Category ticketCategory;

    public TicketManager(DiscordBot parent) {
        this.parent = parent;
        ticketButtonListener = new TicketButtonListener(this);
        parent.addEventListener(ticketButtonListener);

        ticketChannel = parent.getHome().getTextChannelById(parent.getConfig().getTicketChannelId());
        ticketLogChannel = parent.getHome().getTextChannelById(parent.getConfig().getTicketLogChannelId());
        ticketCategory = parent.getHome().getCategoryById(parent.getConfig().getTicketCategoryId());

        sendTicketMessage();
    }

    private void sendTicketMessage() {
        ticketChannel.getHistory().retrievePast(1).queue(messages -> {
            if (messages.isEmpty()) {
                MessageBuilder messageBuilder = new MessageBuilder(EmbedUtil.ticketMessageEmbed());
                messageBuilder.setActionRows(ActionRow.of(ticketButtonListener.getButtons()));
                ticketChannel.sendMessage(messageBuilder.build()).queue();
            }
        });
    }

    /**
     * Creates a ticket and returns the created ticket channel
     */
    public TextChannel openTicket(CachedUser owner, TicketType type) {
        return parent.using(session -> {
            int ticketCount = session.createQuery("from Ticket where owner.id = :id")
                    .setParameter("id", owner.getId()).getResultList().size();

            if (ticketCount >= parent.getConfig().getMaxTicketsPerUser()) {
                throw new CommandFailedException("Du kannst maximal " + parent.getConfig().getMaxTicketsPerUser()
                        + " Tickets gleichzeitig erstellen");
            }

            Ticket ticket = new Ticket();
            ticket.setOwner(owner);
            ticket.setName(type.getId());
            ticket.setType(type);
            ticket.setOpen(true);

            HashSet<CachedUser> users = new HashSet<>();
            users.add(owner);
            ticket.setTicketUsers(users);

            int ticketId = (Integer) session.save(ticket);

            TextChannel textChannel = ticketCategory.createTextChannel(type.getId() + "-" + ticketId).complete();
            ticket.setDiscordChannelId(textChannel.getIdLong());
            session.update(ticket);
            return textChannel;
        });
    }

    @SuppressWarnings("unchecked")
    public Ticket renameTicketByChannel(TextChannel ticketChannel, String name) {
        return parent.using(session -> {
            List<Ticket> tickets = (List<Ticket>) session
                    .createQuery("from Ticket where discordChannelId = :id")
                    .setParameter("id", ticketChannel.getIdLong()).getResultList();

            if (tickets.size() == 0) {
                throw new CommandFailedException("Der Befehl muss in einem Ticket ausgeführt werden!");
            }

            Ticket ticket = tickets.get(0);
            ticket.setName(name);
            session.save(ticket);
            return ticket;
        });
    }

    @SuppressWarnings("unchecked")
    public Ticket changeTypeByChannel(TextChannel ticketChannel, TicketType type) {
        return parent.using(session -> {
            List<Ticket> tickets = (List<Ticket>) session
                    .createQuery("from Ticket where discordChannelId = :id")
                    .setParameter("id", ticketChannel.getIdLong()).getResultList();

            if (tickets.size() == 0) {
                throw new CommandFailedException("Der Befehl muss in einem Ticket ausgeführt werden!");
            }

            Ticket ticket = tickets.get(0);
            ticket.setType(type);
            session.save(ticket);
            return ticket;
        });
    }

    @SuppressWarnings("unchecked")
    public void addUser(TextChannel ticketChannel, CachedUser cachedUser) {
        parent.using(session -> {
            List<Ticket> tickets = (List<Ticket>) session
                    .createQuery("from Ticket where discordChannelId = :id")
                    .setParameter("id", ticketChannel.getIdLong()).getResultList();

            if (tickets.size() == 0) {
                throw new CommandFailedException("Der Befehl muss in einem Ticket ausgeführt werden!");
            }

            Ticket ticket = tickets.get(0);
            ticket.getTicketUsers().add(cachedUser);
            session.saveOrUpdate(ticket);
        });
    }

    @SuppressWarnings("unchecked")
    public void removeUser(TextChannel ticketChannel, CachedUser cachedUser) {
        parent.using(session -> {
            List<Ticket> tickets = (List<Ticket>) session
                    .createQuery("from Ticket where discordChannelId = :id")
                    .setParameter("id", ticketChannel.getIdLong()).getResultList();

            if (tickets.size() == 0) {
                throw new CommandFailedException("Der Befehl muss in einem Ticket ausgeführt werden!");
            }

            // Removing a user does not remove them from the database
            // since the user could have sent a message inside the ticket.
        });
    }

    @SuppressWarnings("unchecked")
    public Ticket closeTicket(TextChannel ticketChannel, CachedUser closeUser, String cause) {
        return parent.using(session -> {
            List<Ticket> tickets = (List<Ticket>) session
                    .createQuery("from Ticket where discordChannelId = :id")
                    .setParameter("id", ticketChannel.getIdLong()).getResultList();

            if (tickets.size() == 0) {
                throw new CommandFailedException("Der Befehl muss in einem Ticket ausgeführt werden!");
            }

            Ticket ticket = tickets.get(0);
            ticket.setOpen(false);
            ticket.setClosedUser(closeUser);
            ticket.setClosedCause(cause);
            ticket.setTicketMessages(getMessagesAndSetUsers(ticket, ticketChannel));
            session.saveOrUpdate(ticket);

            return ticket;
        });
    }

    private List<TicketMessage> getMessagesAndSetUsers(Ticket ticket, TextChannel channel) {
        List<TicketMessage> messageList = new ArrayList<>();
        Set<User> users = new HashSet<>();

        for (Message message : channel.getIterableHistory()) {
            messageList.add(TicketMessage.fromDiscordMessage(message));
            users.add(message.getAuthor());
        }

        ticket.setTicketUsers(users.stream().map(this::updateAndGetMember).collect(Collectors.toSet()));
        return messageList;
    }

    public CachedUser updateAndGetMember(Member member) {
        return parent.using(session -> {
            CachedUser cachedUser = session.get(CachedUser.class, member.getIdLong());

            if (cachedUser == null) {
                cachedUser = new CachedUser(
                        member.getIdLong(),
                        member.getUser().getName(),
                        member.getUser().getDiscriminator(),
                        member.getNickname()
                );
            } else {
                cachedUser.setUsername(member.getUser().getName());
                cachedUser.setDiscriminator(member.getUser().getDiscriminator());
                cachedUser.setNickname(member.getNickname());
            }

            session.saveOrUpdate(cachedUser);
            return cachedUser;
        });
    }

    public CachedUser updateAndGetMember(User invalidMember) {
        return parent.using(session -> {
            CachedUser cachedUser = session.get(CachedUser.class, invalidMember.getIdLong());

            if (cachedUser == null) {
                cachedUser = new CachedUser(
                        invalidMember.getIdLong(),
                        invalidMember.getName(),
                        invalidMember.getDiscriminator(),
                        null
                );
            } else {
                cachedUser.setUsername(invalidMember.getName());
                cachedUser.setDiscriminator(invalidMember.getDiscriminator());
                cachedUser.setNickname(null);
            }

            session.saveOrUpdate(cachedUser);
            return cachedUser;
        });
    }

}
