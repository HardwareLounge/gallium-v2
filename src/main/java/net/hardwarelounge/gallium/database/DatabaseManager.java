package net.hardwarelounge.gallium.database;

import com.mysql.cj.jdbc.Driver;
import lombok.Getter;
import net.hardwarelounge.gallium.DiscordBot;
import org.apache.logging.log4j.LogManager;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQL8Dialect;

public class DatabaseManager {

    @Getter
    private final SessionFactory sessionFactory;

    public DatabaseManager(DiscordBot parent) {
        try {
            sessionFactory = new Configuration()
                    .setProperty("hibernate.connection.driver_class", Driver.class.getName())
                    .setProperty("hibernate.connection.url", String.format(
                            "jdbc:mysql://%s:%s/%s?useSSL=%s",
                            parent.getConfig().getDatabaseHost(),
                            parent.getConfig().getDatabasePort(),
                            parent.getConfig().getDatabase(),
                            parent.getConfig().getDatabaseUseSSL()
                    ))
                    .setProperty("hibernate.connection.username", parent.getConfig().getDatabaseUsername())
                    .setProperty("hibernate.connection.password", parent.getConfig().getDatabasePassword())
                    .setProperty("hibernate.dialect", MySQL8Dialect.class.getName())
                    .setProperty("hibernate.hbm2ddl.auto", "update")
                    .setProperty("hbm2ddl.auto", "update")
                    .setProperty("spring.jpa.hibernate.ddl-auto", "update")
                    .setProperty("javax.persistence.schema-generation.database.action", "update")
                    .setProperty("show_sql", "true")
                    .addAnnotatedClass(Ticket.class)
                    .addAnnotatedClass(CachedUser.class)
                    .addAnnotatedClass(TicketMessage.class)
                    .buildSessionFactory();
        } catch (HibernateException exception) {
            LogManager.getLogger("Hibernate")
                    .error("Could not create Hibernate SessionFactory", exception);
            throw new IllegalStateException();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(sessionFactory::close));
    }

}
