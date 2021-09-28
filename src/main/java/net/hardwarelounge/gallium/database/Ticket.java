package net.hardwarelounge.gallium.database;

import lombok.*;

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

    @OrderBy("id asc")
    @OneToMany(mappedBy = "ticket")
    private List<TicketMessage> discordChannelId;

}
