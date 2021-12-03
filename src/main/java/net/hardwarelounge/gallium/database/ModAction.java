package net.hardwarelounge.gallium.database;

import lombok.*;
import net.hardwarelounge.gallium.punishment.Punishment;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;

// lombok
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
// JPA
@Entity
@Table(name = "mod_action", uniqueConstraints = {
        @UniqueConstraint(columnNames = "id")
})
public class ModAction {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Punishment type;

    @ManyToOne(optional = false)
    private CachedUser performedBy;

    @ManyToOne(optional = false)
    private CachedUser performedOn;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false, nullable = false)
    private Date performedAt;

    @Column(nullable = false)
    private String performedBecause;

    @Column(nullable = false)
    private long duration;

    @Column(nullable = false)
    private boolean pardoned = false;

    @ManyToOne
    private CachedUser pardonedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date pardonedAt;

    @Column
    private String pardonedBecause;

}
