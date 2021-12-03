package net.hardwarelounge.gallium.database;

import lombok.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.hibernate.cache.spi.entry.CacheEntry;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

// lombok
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
// JPA
@Entity
@Table(name = "ticket_message", uniqueConstraints = {
        @UniqueConstraint(columnNames = "id")
})
public class TicketMessage {

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private long id;

    @Column(name = "author_id", nullable = false)
    private String author;

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @ElementCollection
    @Column(name = "embeds")
    private List<String> embeds;

    @ElementCollection
    @Column(name = "attachments")
    private List<String> attachments;

    public static TicketMessage fromDiscordMessage(Message message) {
        return new TicketMessage(
                null,
                message.getIdLong(),
                message.getAuthor().getId(),
                message.getContentRaw(),
                message.getEmbeds().stream()
                        .map(MessageEmbed::toData)
                        .map(DataObject::toString)
                        .toList(),
                message.getAttachments().stream()
                        .map(Message.Attachment::getUrl)
                        .toList()
        );
    }

}
