package org.coin.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.coin.order.entity.BuyOrder;
import org.coin.order.entity.SellOrder;
import org.coin.wallet.entity.Wallet;

import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"username"}
        )
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Setter
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Wallet> wallets;

    @Setter
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<BuyOrder> buyOrders;

    @Setter
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<SellOrder> sellOrders;

    @Setter
    private Double money = 0.;

    @Builder
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void increaseMoney(Double money) {
        this.money += money;
    }

    public void decreaseMoney(Double money) {
        this.money -= money;
    }
}