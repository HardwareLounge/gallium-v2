package net.hardwarelounge.gallium;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.hardwarelounge.gallium.config.GalliumConfig;
import net.hardwarelounge.gallium.config.PermissionConfig;
import net.hardwarelounge.gallium.config.SecurityConfig;
import net.hardwarelounge.gallium.database.DatabaseManager;
import net.hardwarelounge.gallium.listener.SlashCommandListener;
import net.hardwarelounge.gallium.punishment.PunishmentManager;
import net.hardwarelounge.gallium.security.SecurityListener;
import net.hardwarelounge.gallium.ticket.TicketManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class DiscordBot {

    private static final Logger LOGGER = LogManager.getLogger("Gallium");

    private final @Getter GalliumConfig config;
    private final @Getter PermissionConfig permissionConfig;
    private final @Getter SecurityConfig securityConfig;
    private final GalliumScheduledTasks galliumScheduledTasks;

    private @Getter JDA jda;
    private @Getter Guild home;

    private @Getter SlashCommandListener slashCommandListener;
    private @Getter SecurityListener securityListener;

    private @Getter DatabaseManager databaseManager;
    private @Getter TicketManager ticketManager;
    private @Getter PunishmentManager punishmentManager;

    public DiscordBot(GalliumConfig config, PermissionConfig permissionConfig, SecurityConfig securityConfig) {
        this.config = config;
        this.permissionConfig = permissionConfig;
        this.securityConfig = securityConfig;
        this.galliumScheduledTasks = new GalliumScheduledTasks(this);
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
                slashCommandListener,
                securityListener
        );

        initializeManagers();

        LOGGER.info("Finished startup process!");
        jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.watching(getHome().getMemberCount() - 1 + " Users"));

        LOGGER.info("Scheduling tasks...");
        galliumScheduledTasks.scheduleTasks();
    }

    private void initializeEventListeners() {
        slashCommandListener = new SlashCommandListener(this);
        securityListener = new SecurityListener(this);
    }

    private void initializeManagers() {
        databaseManager = new DatabaseManager(this);
        ticketManager = new TicketManager(this);
        punishmentManager = new PunishmentManager(this);
        punishmentManager.pardonExpiredPunishments();
    }

    public void using(Consumer<Session> sessionConsumer) {
        try (Session session = databaseManager.getSessionFactory().openSession()) {
            session.beginTransaction();
            sessionConsumer.accept(session);
            session.getTransaction().commit();
            session.close();
        }
    }

    public <T> T using(Function<Session, T> sessionConsumer) {
        try (Session session = databaseManager.getSessionFactory().openSession()) {
            session.beginTransaction();
            T returnValue = sessionConsumer.apply(session);
            session.getTransaction().commit();
            session.close();
            return returnValue;
        }
    }

    public void addEventListener(ListenerAdapter... eventListener) {
        for (ListenerAdapter listenerAdapter : eventListener) {
            jda.addEventListener(listenerAdapter);
        }
    }

    public Logger getLogger() {
        return LOGGER;
    }

}
