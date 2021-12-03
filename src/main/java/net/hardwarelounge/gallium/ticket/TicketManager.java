package net.hardwarelounge.gallium.ticket;

import lombok.Getter;
import lombok.val;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.archive.ArchiveMessage;
import net.hardwarelounge.gallium.archive.ArchiveRole;
import net.hardwarelounge.gallium.archive.ArchiveUserData;
import net.hardwarelounge.gallium.archive.DiscordChannelArchive;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.Ticket;
import net.hardwarelounge.gallium.database.TicketMessage;
import net.hardwarelounge.gallium.interaction.TicketButtonListener;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;
import net.hardwarelounge.gallium.util.Manager;

import java.util.*;
import java.util.stream.Collectors;

public class TicketManager extends Manager {

    private final TicketButtonListener ticketButtonListener;

    private final MessageChannel ticketChannel;
    private final @Getter MessageChannel ticketLogChannel;
    private final Category ticketCategory;

    public TicketManager(DiscordBot parent) {
        super(parent);
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

            Number ticketId = (Number) session.save(ticket);

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

    public DiscordChannelArchive archiveTicket(Ticket ticket) {
        Map<String, ArchiveRole> roleMap = new HashMap<>();
        Map<String, ArchiveUserData> userData = new HashMap<>();

        roleMap.put("-1", ArchiveRole.builder().name("everyone").color(0x95a5a6).build());

        for (CachedUser ticketUser : ticket.getTicketUsers()) {
            Member member = parent.getHome().getMemberById(ticketUser.getId());

            userData.putIfAbsent(String.valueOf(ticketUser.getId()), ArchiveUserData.builder()
                    .lastUsername(ticketUser.getUsername())
                    .lastTag(ticketUser.getDiscriminator())
                    .lastNickname(ticketUser.getNickname())
                    .topRole(member != null && member.getRoles().size() > 0 ? member.getRoles().get(0).getId() : "-1")
                    .lastAvatar(member != null ? member.getUser().getAvatarUrl() : "")
                    .build()
            );

            if (member != null) {
                for (Role role : member.getRoles()) {
                    roleMap.putIfAbsent(role.getId(), ArchiveRole.builder()
                            .name(role.getName())
                            .color(role.getColorRaw())
                            .build());
                }
            }
        }

        List<ArchiveMessage> messages = ticket.getTicketMessages()
                .stream()
                .map(ticketMessage -> ArchiveMessage.builder()
                        .id(String.valueOf(ticketMessage.getId()))
                        .author(ticketMessage.getAuthor())
                        .content(ticketMessage.getContent())
                        .embeds(ticketMessage.getEmbeds().toArray(new String[0]))
                        .attachments(ticketMessage.getAttachments().toArray(new String[0]))
                        .build())
                .toList();

        return DiscordChannelArchive.builder()
                .name(ticket.getName())
                .id(String.valueOf(ticket.getId()))
                .topic(ticket.getType().getName() + " Ticket von " + ticket.getOwner().getUsername() + ticket.getOwner().getDiscriminator())
                .roles(roleMap)
                .userData(userData)
                .messages(messages.toArray(new ArchiveMessage[0]))
                .build();
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

}
