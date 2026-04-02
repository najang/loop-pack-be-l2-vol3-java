package com.loopers.domain.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntryTokenService {

    private static final String TOKEN_KEY_PREFIX = "entry-token:";
    static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    @Qualifier("redisTemplateMaster")
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 입장 토큰을 발급한다. TTL: 5분
     *
     * @return 발급된 토큰 값
     */
    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(tokenKey(userId), token, TOKEN_TTL);
        return token;
    }

    /**
     * 토큰이 유효한지 검증한다.
     */
    public boolean validate(Long userId, String token) {
        String stored = redisTemplate.opsForValue().get(tokenKey(userId));
        return token != null && token.equals(stored);
    }

    /**
     * 토큰을 삭제한다. (주문 완료 후 호출)
     */
    public void delete(Long userId) {
        redisTemplate.delete(tokenKey(userId));
    }

    /**
     * 토큰이 존재하는지 확인한다.
     *
     * @return 토큰 값, 없으면 Optional.empty()
     */
    public Optional<String> findToken(Long userId) {
        String token = redisTemplate.opsForValue().get(tokenKey(userId));
        return Optional.ofNullable(token);
    }

    private String tokenKey(Long userId) {
        return TOKEN_KEY_PREFIX + userId;
    }
}
