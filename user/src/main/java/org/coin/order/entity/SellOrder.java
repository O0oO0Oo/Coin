package org.coin.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.coin.common.entity.BaseTimeEntity;
import org.coin.crypto.entity.Crypto;
import org.coin.user.entity.User;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellOrder extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sell_order_id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

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
    public SellOrder(User user, Crypto crypto, Double quantity, Double price, boolean processed) {
        this.user = user;
        this.crypto = crypto;
        this.quantity = quantity;
        this.price = price;
        this.processed = processed;
    }
}