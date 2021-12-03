package net.hardwarelounge.gallium.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;

public class Manager {

    protected final DiscordBot parent;

    public Manager(DiscordBot parent) {
        this.parent = parent;
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
