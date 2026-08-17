package com.stockdemy.global.security;

import com.stockdemy.domain.user.entity.User;
import com.stockdemy.domain.user.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 사용자 토큰 버전 캐시 */
@Component
@RequiredArgsConstructor
public class TokenVersionStore {

  private static final String KEY_PREFIX = "token-version:";
  private static final Duration TTL = Duration.ofDays(30);

  private final StringRedisTemplate redisTemplate;
  private final UserRepository userRepository;

  // 현재 토큰 버전 조회
  public int getCurrentVersion(Long userId) {

    // 캐시 조회
    String cached = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
    if (cached != null) return Integer.parseInt(cached);

    // DB 조회
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + userId));

    refresh(userId, user.getTokenVersion());
    return user.getTokenVersion();
  }

  // 토큰 버전 갱신
  public void refresh(Long userId, int version) {
    redisTemplate.opsForValue().set(KEY_PREFIX + userId, String.valueOf(version), TTL);
  }
}
