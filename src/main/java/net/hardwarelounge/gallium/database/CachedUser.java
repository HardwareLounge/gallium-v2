package net.hardwarelounge.gallium.database;

import lombok.*;

import javax.persistence.*;

// lombok
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// JPA
@Entity
@Table(name = "cached_user", uniqueConstraints = {
        @UniqueConstraint(columnNames = "id")
})
public class CachedUser {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private long id;

    @Column(name = "username", nullable = false, length = 32)
    private String username;

    @Column(name = "discriminator", nullable = false, length = 4)
    private String discriminator;

    @Column(name = "nickname", nullable = true, length = 32)
    private String nickname;

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CachedUser user) && user.id == this.id;
    }

    @Override
    public String toString() {
        return id + "(" + username + "#" + discriminator + ")";
    }

    public String getAsMention() {
        return "<@!" + id + ">";
    }
}
