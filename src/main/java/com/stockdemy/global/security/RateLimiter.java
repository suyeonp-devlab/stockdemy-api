package com.stockdemy.global.security;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 범용 요청 제한 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

  private final StringRedisTemplate redisTemplate;

  // 쿨타임 확인 및 설정
  public boolean tryAcquireCooldown(String key, Duration cooldown) {
    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", cooldown);
    return Boolean.TRUE.equals(acquired);
  }

  // 남은 TTL 초 반환
  public long getRemainingSeconds(String key) {
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    return (ttl == null || ttl < 0L) ? 0L : ttl;
  }

  // 시간 범위 내 사용량 제한
  public boolean tryConsume(String key, int maxCount, Duration window) {
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) redisTemplate.expire(key, window);
    return count != null && count <= maxCount;
  }
}
