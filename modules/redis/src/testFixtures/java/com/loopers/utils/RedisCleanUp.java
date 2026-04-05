package com.loopers.utils;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisCleanUp {
    private final RedisConnectionFactory masterConnectionFactory;

    public RedisCleanUp(@Qualifier("redisConnectionMaster") RedisConnectionFactory masterConnectionFactory) {
        this.masterConnectionFactory = masterConnectionFactory;
    }

    public void truncateAll(){
        try (RedisConnection connection = masterConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }
}
