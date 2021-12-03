package net.hardwarelounge.gallium.database;

import lombok.*;
import net.hardwarelounge.gallium.ticket.TicketType;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

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
    private long id;

    @ManyToOne(optional = false)
    private CachedUser owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean open;

    @Column
    private Long discordChannelId;

    @ManyToMany
    private Set<CachedUser> ticketUsers;

    @ManyToOne
    private CachedUser closedUser;

    @Column
    private String closedCause;

    @OrderBy("id asc")
    @OneToMany(mappedBy = "ticket")
    private List<TicketMessage> ticketMessages;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType type;

}
