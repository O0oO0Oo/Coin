package org.coin.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.coin.common.entity.BaseTimeEntity;
import org.coin.crypto.entity.Crypto;
import org.coin.user.entity.User;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BuyOrder extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "buy_order_id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Setter
    @ManyToOne
    @JoinColumn(name = "crypto_id")
    private Crypto crypto;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double price;

    @Setter
    private boolean processed = false;

    @Setter
    private boolean canceled = false;

    @Builder
    public BuyOrder(User user, Crypto crypto, Double quantity, Double price, boolean processed) {
        this.user = user;
        this.crypto = crypto;
        this.quantity = quantity;
        this.price = price;
        this.processed = processed;
    }
}