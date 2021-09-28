package net.hardwarelounge.gallium;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.hardwarelounge.gallium.config.GalliumConfig;
import net.hardwarelounge.gallium.database.DatabaseManager;
import net.hardwarelounge.gallium.listener.SlashCommandListener;
import net.hardwarelounge.gallium.ticket.TicketManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.function.Consumer;

public class DiscordBot {

    private static final Logger LOGGER = LogManager.getLogger("Gallium");

    private final @Getter GalliumConfig config;

    private @Getter JDA jda;
    private @Getter Guild home;

    private @Getter SlashCommandListener slashCommandListener;

    private @Getter DatabaseManager databaseManager;
    private @Getter TicketManager ticketManager;

    public DiscordBot(GalliumConfig config) {
        this.config = config;
    }

    public void start() throws LoginException, InterruptedException {
        LOGGER.info("Starting JDA...");
        jda = JDABuilder
                .create(config.getToken(), Arrays.asList(GatewayIntent.values()))
                .setStatus(OnlineStatus.IDLE)
                .setActivity(Activity.playing("starting..."))
                .build();

        jda.awaitReady();
        LOGGER.info("Logged in as " + jda.getSelfUser().getAsTag());
        home = jda.getGuildById(config.getHomeGuildId());

        initializeEventListeners();
        jda.addEventListener(
                slashCommandListener
        );

        initializeManagers();

        LOGGER.info("Finished startup process!");
        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching(getHome().getMemberCount() - 1 + " Users"));
    }

    private void initializeEventListeners() {
        slashCommandListener = new SlashCommandListener(this);
    }

    private void initializeManagers() {
        databaseManager = new DatabaseManager(this);
        ticketManager = new TicketManager(this);
    }

    public void using(Consumer<Session> sessionConsumer) {
        Session session = databaseManager.getSessionFactory().openSession();
        session.beginTransaction();
        sessionConsumer.accept(session);
        session.getTransaction().commit();
        session.close();
    }

}
