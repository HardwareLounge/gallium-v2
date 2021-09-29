package net.hardwarelounge.gallium.database;

import lombok.*;
import net.hardwarelounge.gallium.ticket.TicketType;

import javax.persistence.*;
import java.util.List;

// lombok
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// JPA
@Entity
@Table(name = "ticket", uniqueConstraints = {
        @UniqueConstraint(columnNames = "id")
})
public class Ticket {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private CachedUser owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean open;

    @Column
    private Long discordChannelId;

    @ManyToMany
    private List<CachedUser> ticketUsers;

    @OrderBy("id asc")
    @OneToMany(mappedBy = "ticket")
    private List<TicketMessage> ticketMessages;

    @Enumerated(EnumType.STRING)
    private TicketType type;

}
