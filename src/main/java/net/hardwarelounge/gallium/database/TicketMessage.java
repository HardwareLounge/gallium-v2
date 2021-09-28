package net.hardwarelounge.gallium.database;

import lombok.*;

import javax.persistence.*;

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

    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    @Column(name = "attachments", nullable = false, length = 4000)
    private String attachments;

}
