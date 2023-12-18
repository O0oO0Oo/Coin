package org.coin.trade.config;

import io.lettuce.core.ReadFrom;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStaticMasterReplicaConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
public class WriteToMasterReadFromReplicaConfig {
    private RedisInstance master;
    private List<RedisInstance> slaves;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();

        RedisStaticMasterReplicaConfiguration masterReplicaConfiguration =
                new RedisStaticMasterReplicaConfiguration(getMaster().host, getMaster().port);
        this.getSlaves().forEach(slave -> masterReplicaConfiguration.addNode(slave.host, slave.port));

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(masterReplicaConfiguration, clientConfiguration);
        lettuceConnectionFactory.afterPropertiesSet();
        return lettuceConnectionFactory;
    }

    @Data
    private static class RedisInstance {
        private String host;
        private int port;
    }
}