package net.hardwarelounge.gallium.command;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.database.CachedUser;
import net.hardwarelounge.gallium.database.ModAction;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModerationCommands {

    public static class ModLogCommand extends SlashCommand {
        public ModLogCommand(DiscordBot parent, String uniqueName) {
            super(parent, uniqueName);
        }

        @Override
        public CommandData commandData() {
            return new CommandData("mod-log", "View the moderation record of a user")
                    .addOption(OptionType.USER, "target", "The target user", true);
        }

        @Override
        public void execute(SlashCommandEvent event) {
            User targetUser = Objects.requireNonNull(event.getOption("target")).getAsUser();
            CachedUser target = parent.getPunishmentManager().updateAndGetMember(targetUser);
            List<ModAction> history = parent.getPunishmentManager().listPunishments(target);

            if (history.size() == 0) {
                event.replyEmbeds(
                        EmbedUtil.defaultEmbed()
                                .setDescription(target.toString() + " has no mod actions on their record.")
                                .build()
                ).queue();
            } else if (history.size() > 5) {
                // shorter format if user has more than 5 mod actions on their record
                StringBuilder message = new StringBuilder();

                for (ModAction action : history) {
                    message.append(String.format("`%s` for `%ss` by `%s` at `%s` because `%s`",
                            action.getType().name(),
                            action.getDuration(),
                            action.getPerformedBy().toString(),
                            action.getPerformedAt().toString(),
                            action.getPerformedBecause()
                    ));

                    if (action.isPardoned()) {
                        message.append(String.format(" pardoned by `%s` at `%s` because `%s`",
                                action.getPardonedBy(),
                                action.getPardonedAt(),
                                action.getPardonedBecause()
                        ));
                    }

                    message.append("\n");
                }

                event.reply(String.format("%s\n\n%s",
                        String.format("\\* __ModAction record for target %s(%s)__ \\*", target.getId(), target.getUsername() + "#" + target.getDiscriminator()),
                        message
                )).setEphemeral(false).queue();
            } else {
                // regular embed format
                List<MessageEmbed> embeds = new ArrayList<>(5);
                for (ModAction action : history) embeds.add(EmbedUtil.modActionEmbed(action).build());
                event.replyEmbeds(embeds).setEphemeral(false).queue();
            }
        }
    }

    public static class PardonAllCommand extends SlashCommand {
        public PardonAllCommand(DiscordBot parent, String uniqueName) {
            super(parent, uniqueName);
        }

        @Override
        public CommandData commandData() {
            return new CommandData("pardon-all", "Pardons all active bans and mutes")
                    .addOption(OptionType.USER, "target", "The target user", true)
                    .addOption(OptionType.STRING, "cause", "Cause for the pardon", true);
        }

        @Override
        public void execute(SlashCommandEvent event) {
            OptionMapping targetOption = event.getOption("target");
            OptionMapping causeOption = event.getOption("cause");
            if (targetOption == null || causeOption == null) throw new CommandFailedException();
            if (causeOption.getAsString().isBlank()) throw new CommandFailedException("Gib einen Grund an!");

            Member target = targetOption.getAsMember();
            if (target == null) throw new CommandFailedException("User nicht gefunden!");
            CachedUser cachedTarget = parent.getPunishmentManager().updateAndGetMember(target);

            parent.getPunishmentManager().pardonAll(
                    cachedTarget,
                    parent.getPunishmentManager().updateAndGetMember(event.getMember()),
                    targetOption.getAsString()
            );

            event.replyEmbeds(EmbedUtil.defaultEmbed()
                    .setDescription("Alle Verwarnungen und Mutes von " + cachedTarget + " aufgehoben.")
                    .build()
            ).queue();
        }
    }

    public static class PardonCommand extends SlashCommand {
        public PardonCommand(DiscordBot parent, String uniqueName) {
            super(parent, uniqueName);
        }

        @Override
        public CommandData commandData() {
            return new CommandData("pardon", "Pardon a specific punishment")
                    .addOption(OptionType.INTEGER, "action-id", "The punishment to pardon", true)
                    .addOption(OptionType.STRING, "cause", "Cause for the pardon", true);
        }

        @Override
        public void execute(SlashCommandEvent event) {
            OptionMapping idOption = event.getOption("action-id");
            OptionMapping causeOption = event.getOption("cause");
            if (idOption == null || causeOption == null) throw new CommandFailedException();

            ModAction modAction = parent.getPunishmentManager().pardon(
                    idOption.getAsLong(),
                    parent.getPunishmentManager().updateAndGetMember(event.getMember()),
                    causeOption.getAsString()
            );

            if (modAction == null) {
                event.reply("Pardon ID " + idOption.getAsString() + " nicht gefunden!")
                        .setEphemeral(true).queue();
            } else {
                event.replyEmbeds(EmbedUtil.modActionEmbed(modAction).build()).setEphemeral(true).queue();
            }
        }
    }

}
