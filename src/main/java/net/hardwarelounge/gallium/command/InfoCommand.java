package net.hardwarelounge.gallium.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.hardwarelounge.gallium.DiscordBot;
import net.hardwarelounge.gallium.util.CommandFailedException;
import net.hardwarelounge.gallium.util.EmbedUtil;

public class InfoCommand extends SlashCommand {

    public InfoCommand(DiscordBot parent, String uniqueName) {
        super(parent, uniqueName);
    }

    @Override
    public CommandData commandData() {
        return new CommandData("info", "Zeigt diverse Informationen").addSubcommands(
                new SubcommandData("bot", "Die (grobe) Hilfeseite des Bots"),
                new SubcommandData("user", "Zeigt Informationen über einen User")
                        .addOption(OptionType.USER, "user", "Der Ziel-User", true),
                new SubcommandData("server", "Zeigt Informationen zum aktuellen Server")
        );
    }

    @Override
    public void execute(SlashCommandEvent event) {
        switch (event.getSubcommandName()) {
            case "bot" -> event.reply("").addEmbeds(EmbedUtil.defaultEmbed().build()).queue();

            case "user" -> {
                User user = event.getOption("user").getAsUser();
                Member member = parent.getHome().getMember(user);

                EmbedBuilder builder = EmbedUtil.defaultEmbed()
                        .setTitle(user.getAsTag())
                        .setThumbnail(user.getAvatarUrl())
                        .setDescription(user.getId())
                        .addField("Account erstellt", user.getTimeCreated().toString(), false);

                if (member != null) {
                    builder.addField("Server beigetreten", member.getTimeJoined().toString(), false)
                            .addField("Server ge-boosted", member.getTimeBoosted() != null ?
                                    member.getTimeBoosted().toString() : "kein Server-Booster", false)
                            .addField("Nickname", member.getNickname(), false);
                }

                event.reply("").addEmbeds(builder.build()).queue();
            }

            case "server" -> {
                event.reply(parent.getHome().getName()).queue();
            }

            default -> throw new CommandFailedException();
        }
    }
}
