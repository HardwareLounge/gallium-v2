package net.hardwarelounge.gallium.security;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.punishment.Punishment;
import net.hardwarelounge.gallium.util.DiscordRoleColors;
import net.hardwarelounge.gallium.util.EmbedUtil;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class SecurityListener extends ListenerAdapter {

    @Getter
    private final DiscordBot parent;

    private final Trie spamPhraseMatcher;
    private final Trie blacklistPhraseMatcher;

    private final Map<Long, Integer> spamHistory;
    private final Map<Long, Integer> blacklistHistory;

    public SecurityListener(DiscordBot parent) {
        this.parent = parent;

        spamPhraseMatcher = Trie.builder()
                .ignoreCase()
                .ignoreOverlaps()
                .addKeywords(getParent().getSecurityConfig().getPotentialSpamPhrases())
                .build();

        blacklistPhraseMatcher = Trie.builder()
                .ignoreCase()
                .stopOnHit()
                .addKeywords(getParent().getSecurityConfig().getBlacklistedPhrases())
                .build();

        spamHistory = new TreeMap<>();
        blacklistHistory = new TreeMap<>();
    }

    public void clearHistoryMaps() {
        synchronized (spamHistory) {
            spamHistory.clear();
        }

        synchronized (blacklistHistory) {
            blacklistHistory.clear();
        }
    }

    private void autoRoleBan(Member member, String cause) {
        parent.getPunishmentManager().punish(
                parent.getPunishmentManager().updateAndGetMember(member),
                parent.getPunishmentManager().updateAndGetMember(parent.getJda().getSelfUser()),
                Punishment.ROLE_BAN,
                3600 * 24 * 14,
                cause
        );
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()
                || event.getAuthor().isSystem()
                || Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR)) {
            return;
        }

        Collection<Emit> spamMatches = spamPhraseMatcher.parseText(event.getMessage().getContentRaw());
        Collection<Emit> blacklistMatches = blacklistPhraseMatcher.parseText(event.getMessage().getContentRaw());

        boolean deleteMessage = false;

        if (blacklistMatches.size() > 0) {
            deleteMessage = true;

            parent.getPunishmentManager().getModLogChannel().sendMessageEmbeds(EmbedUtil.defaultEmbed()
                    .setTitle("BLACKLIST MATCH").setColor(DiscordRoleColors.RED.getSecondary())
                    .setDescription("User " + event.getAuthor() + " sent blacklisted word(s):\n\n"
                            + blacklistMatches.stream().map(Emit::getKeyword).collect(Collectors.joining(", ")))
                    .build()
            ).queue();

            synchronized (blacklistHistory) {
                int blacklistHits = blacklistHistory.getOrDefault(event.getAuthor().getIdLong(), 0);
                blacklistHistory.put(event.getAuthor().getIdLong(), blacklistHits + 1);

                if (blacklistHits >= parent.getSecurityConfig().getBlacklistRepeatAutoLimit()) {
                    autoRoleBan(event.getMember(), "Blacklist-Wörter zu oft verwendet");
                }
            }
        }

        if (spamMatches.size() >= parent.getSecurityConfig().getSpamWordCountAutoMod()) {
            deleteMessage = true;

            synchronized (spamHistory) {
                int spamHits = spamHistory.getOrDefault(event.getAuthor().getIdLong(), 1);
                spamHistory.put(event.getAuthor().getIdLong(), spamHits + 1);

                event.getChannel().sendMessage(new MessageBuilder()
                        .setContent(event.getMessage().getAuthor().getAsMention())
                        .setEmbeds(EmbedUtil.defaultEmbed()
                                .setTitle("Zu viele Treffer!")
                                .setDescription(String.format(
                                        "Halte die Anzahl folgender Wörter unter %s: `%s`. Bitte warte fünf Minuten "
                                                + "bevor du erneut versuchst Nachrichten mit diesen Wörtern zu versenden.",
                                        parent.getSecurityConfig().getSpamWordCountAutoMod(),
                                        spamMatches.stream().map(Emit::getKeyword)
                                                .collect(Collectors.joining("`, `"))
                                )).build())
                        .build()).queue();
                if (spamHits >= 2) {
                    autoRoleBan(event.getMember(), "Spam Protection");
                }
            }
        }

        if (deleteMessage) event.getMessage().delete().queue();
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (event.getUser().getTimeCreated().isBefore(OffsetDateTime.now()
                .minus(parent.getSecurityConfig().getMinAccountAgeSeconds(), ChronoUnit.SECONDS))) {
            try {
                event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel
                        .sendMessage("Dein Account ist weniger als `"
                                + humanReadableFormat(Duration.ofSeconds(
                                parent.getSecurityConfig().getMinAccountAgeSeconds()))
                                + "` alt. Bitte habe noch etwas Geduld, diese Maßnahme wird aufgrund der vielen"
                                + " Spam Attacken auf Discord durchgesetzt.")
                        .queue());
            } catch (ErrorResponseException exception) {
                log.info("Could not message user " + event.getUser() + ", their DMs are closed");
            }

            log.info("Kicking user " + event.getUser() + " because their account is too new");
            event.getMember().kick("Account zu neu, versuche es in " + humanReadableFormat(
                    Duration.ofSeconds(ChronoUnit.SECONDS.between(
                                event.getUser().getTimeCreated().plus(Duration.ofSeconds(parent
                                        .getSecurityConfig().getMinAccountAgeSeconds())),
                                OffsetDateTime.now()
                    ))
            ) + " erneut.").queue();
        }
    }

    private static String humanReadableFormat(Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

}
