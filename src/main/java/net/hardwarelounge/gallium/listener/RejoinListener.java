package net.hardwarelounge.gallium.listener;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.hardwarelounge.gallium.DiscordBot;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class RejoinListener extends ListenerAdapter {

    private final DiscordBot parent;

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        parent.getPunishmentManager().assignPunishmentRoles(event.getMember());
    }
}
