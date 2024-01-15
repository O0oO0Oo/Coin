package org.coin.trade.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.connection.balancer.RoundRobinLoadBalancer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * TODO : sentinel
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "redis")
@EnableConfigurationProperties
public class RedissonWriteToMasterReadFromReplicaConfiguration {
    private RedisInstance master;
    private List<RedisInstance> slaves;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setCodec(StringCodec.INSTANCE);
        config.useMasterSlaveServers()
                .setMasterAddress(master.toString())
                .setReadMode(ReadMode.MASTER)
                .setLoadBalancer(new RoundRobinLoadBalancer());
        slaves
                .forEach(slave -> config.useMasterSlaveServers()
                        .addSlaveAddress(slave.toString()));

        return Redisson.create(config);
    }

    @Data
    private static class RedisInstance {
        private String host;
        private int port;

        @Override
        public String toString() {
            return "redis://" + host + ":" + port;
        }
    }
}