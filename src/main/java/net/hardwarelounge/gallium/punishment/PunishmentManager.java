package net.hardwarelounge.gallium.punishment;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.ModAction;
import net.hardwarelounge.gallium.util.Manager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PunishmentManager extends Manager {

    private final Map<Punishment, Role> punishmentRoleMap;

    public PunishmentManager(DiscordBot parent) {
        super(parent);
        punishmentRoleMap = loadRoles();
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
        return parent.using(session -> {
            ModAction action = new ModAction();
            action.setPerformedOn(target);
            action.setPerformedAt(Date.from(Instant.now()));
            action.setPerformedBy(moderator);
            action.setType(type);
            action.setDuration(duration);
            action.setPerformedBecause(cause);

            session.save(action);
            return action;
        });
    }

    public void pardonAll(CachedUser target, CachedUser moderator, String cause) {
        List<ModAction> modActions = listPunishments(target);
        parent.using(session -> {
            parent.getLogger().info("Pardoning all mutes and bans from {}", target);
            for (ModAction modAction : modActions) {
                modAction.setPardonedAt(Date.from(Instant.now()));
                modAction.setPardonedBy(moderator);
                modAction.setPardonedBecause(cause);
                session.saveOrUpdate(modAction);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public List<ModAction> listPunishments(CachedUser user) {
        return (List<ModAction>) parent.using(session -> {
            return session.createQuery("from ModAction where performedOn = :user")
                    .setParameter("user", user)
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
                            session.saveOrUpdate(action);

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

}
