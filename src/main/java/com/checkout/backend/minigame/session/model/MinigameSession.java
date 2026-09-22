package com.checkout.backend.minigame.session.model;

import com.checkout.backend.minigame.model.Minigame;
import com.checkout.backend.user.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One play of one minigame by one user.
 *
 * This is the associative entity that resolves the many-to-many between User
 * and Minigame. It is not optional: the score, the reward and the timestamp
 * belong to the play, not to either side.
 */
@Entity
@Table(
        name = "minigame_sessions",
        indexes = @Index(name = "idx_minigame_sessions_user_played",
                columnList = "user_id, played_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_minigame_sessions_user"))
    private User user;

    /** No cascade: deleting a play must not delete the game for everyone. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "minigame_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_minigame_sessions_minigame"))
    private Minigame minigame;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer score;

    @NotNull
    @DecimalMin("0")
    @Column(name = "tokens_earned", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal tokensEarned = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;

    /** Identity is the primary key; two unsaved instances are only equal to themselves. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinigameSession other)) return false;
        return id != null && id.equals(other.id);
    }

    /** Constant on purpose: the hash must not change when the id is assigned on persist. */
    @Override
    public int hashCode() {
        return MinigameSession.class.hashCode();
    }
}
