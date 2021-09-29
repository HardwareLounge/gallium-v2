package net.hardwarelounge.gallium.ticket;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.interaction.TicketButtonListener;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;

import java.util.ArrayList;
import java.util.List;

public class TicketManager {

    private final DiscordBot parent;
    private final TicketButtonListener ticketButtonListener;

    private final MessageChannel ticketChannel;
    private final MessageChannel ticketLogChannel;
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

            ArrayList<CachedUser> users = new ArrayList<>();
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

}
