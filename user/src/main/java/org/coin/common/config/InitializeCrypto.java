package org.coin.common.config;

import lombok.RequiredArgsConstructor;
import org.coin.crypto.entity.Crypto;
import org.coin.crypto.repository.CryptoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class InitializeCrypto implements CommandLineRunner {
    private final CryptoRepository cryptoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (cryptoRepository.count() == 0) {
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("BaseCryptoList.txt")){
                List<String> lines = new BufferedReader(
                        new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))
                        .lines().toList();
                cryptoRepository.saveAll(
                        lines.stream()
                                .map(Crypto::new)
                                .toList()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
