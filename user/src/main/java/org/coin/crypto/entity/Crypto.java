package org.coin.crypto.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crypto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crypto_id", nullable = false)
    private Long id;
    private String name;
    @Setter
    private boolean active = true;

    public Crypto(String name) {
        this.name = name;
    }

    public void reverseActive() {
        this.active = !this.active;
    }
}