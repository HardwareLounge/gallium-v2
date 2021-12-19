package net.hardwarelounge.gallium.punishment;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.ModAction;
import net.hardwarelounge.gallium.util.EmbedUtil;
import net.hardwarelounge.gallium.util.Manager;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class PunishmentManager extends Manager {

    private final Map<Punishment, Role> punishmentRoleMap;
    private final @Getter TextChannel modLogChannel;

    public PunishmentManager(DiscordBot parent) {
        super(parent);
        punishmentRoleMap = loadRoles();
        modLogChannel = Objects.requireNonNull(parent.getHome()
                .getTextChannelById(parent.getConfig().getModLogChannelId()));
    }

    private Map<Punishment, Role> loadRoles() {
        Map<Punishment, Role> result = new EnumMap<>(Punishment.class);

        for (Punishment type : Punishment.values()) {
            Role punishmentRole = parent.getHome().getRoleById(parent.getPermissionConfig().getRoles()
                    .get(type.name().toLowerCase()).getDiscordRoleId());
            if (punishmentRole == null) {
                parent.getLogger().error("Could not find discord role for punishment type!");
            } else {
                result.put(type, punishmentRole);
            }
        }

        return result;
    }

    @NotNull
    public Role getRole(Punishment punishment) {
        return punishmentRoleMap.get(punishment);
    }

    public ModAction punish(CachedUser target, CachedUser moderator,
                            Punishment type, long duration, String cause) {
        log.info("Punishing user " + target + " with " + type.name() + " for " + duration + "s");
        return parent.using(session -> {
            ModAction action = new ModAction();
            action.setPerformedOn(target);
            action.setPerformedAt(Date.from(Instant.now()));
            action.setPerformedBy(moderator);
            action.setType(type);
            action.setDuration(duration);
            action.setPerformedBecause(cause);

            session.save(action);
            logPunishment(action);
            return action;
        });
    }

    public void pardonAll(CachedUser target, CachedUser moderator, String cause) {
        List<ModAction> modActions = listMutesBans(target);
        parent.using(session -> {
            parent.getLogger().info("Pardoning all mutes and bans from {}", target);
            for (ModAction modAction : modActions) {
                updatePardonedAction(moderator, cause, session, modAction);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public ModAction pardon(long id, CachedUser moderator, String cause) {
        return parent.using(session -> {
            Optional<ModAction> optionalAction = session.createQuery("from ModAction where id = :id")
                    .setParameter("id", id)
                    .getResultStream()
                    .findAny();

            if (optionalAction.isEmpty()) {
                return null;
            } else {
                ModAction action = optionalAction.get();
                updatePardonedAction(moderator, cause, session, action);

                return action;
            }
        });
    }

    private void updatePardonedAction(CachedUser moderator, String cause, Session session, ModAction modAction) {
        modAction.setPardoned(true);
        modAction.setPardonedAt(Date.from(Instant.now()));
        modAction.setPardonedBy(moderator);
        modAction.setPardonedBecause(cause);
        session.saveOrUpdate(modAction);

        session.getTransaction().commit(); // push everything to db
        session.beginTransaction();

        tryRemovePunishmentRole(modAction.getPerformedOn(), modAction.getType());
        logPardon(modAction);
    }

    @SuppressWarnings("unchecked")
    public List<ModAction> listPunishments(CachedUser user) {
        return (List<ModAction>) parent.using(session -> {
            return session.createQuery("from ModAction where performedOn = :user")
                    .setParameter("user", user)
                    .getResultList();
        });
    }

    @SuppressWarnings("unchecked")
    public List<ModAction> listMutesBans(CachedUser user) {
        return (List<ModAction>) parent.using(session -> {
            return session.createQuery("from ModAction where performedOn = :user and pardoned = false and (type = :a or type = :b)")
                    .setParameter("user", user)
                    .setParameter("a", Punishment.MUTE)
                    .setParameter("b", Punishment.ROLE_BAN)
                    .getResultList();
        });
    }

    public void tryRemovePunishmentRole(CachedUser user, Punishment type) {
        parent.using(session -> {
            long activePunishments = session
                    .createQuery("from ModAction where performedOn = :user and type = :type and pardoned = false")
                    .setParameter("user", user)
                    .setParameter("type", type)
                    .getResultList().size();

            parent.getLogger().debug("try removing ({} remaining) {} from {}", activePunishments, type.name(), user);

            if (activePunishments == 0) {
                Member member = parent.getHome().getMember(User.fromId(user.getId()));

                if (member == null) {
                    parent.getLogger().warn("A {} Punishment for {} expired, but user has left the server",
                            type.name(), user);
                } else try {
                    Role punishmentRole = getRole(type);
                    parent.getLogger().info("Removing punishment role {} from {}", type.name(), user);
                    parent.getHome().removeRoleFromMember(member, Objects.requireNonNull(punishmentRole)).queue();
                } catch (RuntimeException exception) {
                    parent.getLogger().error("Runtime exception while trying to remove punishment role!", exception);
                }
            } else {
                parent.getLogger().debug("Did not remove punishment role {} from {}, " +
                                "as they still have active punishments", type.name(), user);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public int pardonExpiredPunishments() {
        return parent.using(session -> {
            AtomicInteger pardonedCount = new AtomicInteger();
            Set<UserPunishment> expiredActions = new HashSet<>();

            session.createQuery("from ModAction where pardoned = false")
                    .getResultStream()
                    .forEach(o -> {
                        ModAction action = (ModAction) o;
                        int difference = action.getPerformedAt().toInstant()
                                .plus(Duration.ofSeconds(action.getDuration()))
                                .compareTo(Instant.now());

                        if (difference <= 0) {
                            parent.getLogger().info("Pardoned ModAction #{} because it expired.", action.getId());
                            action.setPardoned(true);
                            action.setPardonedBecause("Automatic pardon after action expired.");
                            session.saveOrUpdate(action);
                            logPardon(action);

                            pardonedCount.getAndIncrement();
                            expiredActions.add(new UserPunishment(action.getPerformedOn(), action.getType()));
                        }
                    });

            session.getTransaction().commit();
            session.beginTransaction();

            for (UserPunishment o : expiredActions) {
                tryRemovePunishmentRole(o.user(), o.punishment());
            }

            return pardonedCount.get();
        });
    }

    private record UserPunishment(CachedUser user, Punishment punishment) {}

    @SuppressWarnings("unchecked")
    public void assignPunishmentRoles(Member member) {
        CachedUser user = super.updateAndGetMember(member);
        parent.using(session -> {
            session
                    .createQuery("from ModAction where performedOn = :user and pardoned = false")
                    .setParameter("user", user)
                    .getResultStream()
                    .map(action -> ((ModAction) action).getType())
                    .distinct()
                    .forEach(type -> parent.getHome().addRoleToMember(member, getRole((Punishment) type)).queue());
        });
    }

    public void logPunishment(ModAction action) {
        getModLogChannel().sendMessageEmbeds(EmbedUtil.logPunishment(action).build()).queue();
    }

    public void logPardon(ModAction action) {
        getModLogChannel().sendMessageEmbeds(EmbedUtil.logPardon(action).build()).queue();
    }

}
