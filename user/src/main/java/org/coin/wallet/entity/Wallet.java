package org.coin.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import org.coin.crypto.entity.Crypto;
import org.coin.user.entity.User;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "crypto_id"}
        )
)
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "crypto_id")
    private Crypto crypto;

    @Setter
    private Double quantity = 0.;

    @Builder
    public Wallet(User user, Crypto crypto, Double quantity) {
        this.user = user;
        this.crypto = crypto;
        this.quantity = quantity;
    }

    public void increaseQuantity(Double quantity) {
        this.quantity += quantity;
    }

    public void decreaseQuantity(Double quantity) {
        this.quantity -= quantity;
    }
}