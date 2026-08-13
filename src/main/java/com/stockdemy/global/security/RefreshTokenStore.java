package com.stockdemy.global.security;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import com.stockdemy.global.security.dto.RefreshLookupResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Refresh Token 발급 및 검증저장소 */
@Component
public class RefreshTokenStore {

  private static final String TOKEN_KEY_PREFIX = "refresh:";
  private static final String USER_TOKENS_KEY_PREFIX = "refresh:user:";
  private static final String REVOKED_PREFIX = "revoked:";
  private static final Duration REVOKED_TOMBSTONE_TTL = Duration.ofMinutes(5);

  private final StringRedisTemplate redisTemplate;
  private final long refreshTokenValiditySeconds;

  public RefreshTokenStore(
    StringRedisTemplate redisTemplate,
    @Value("${jwt.refresh-token-validity-seconds}") long refreshTokenValiditySeconds
  ) {
    this.redisTemplate = redisTemplate;
    this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
  }

  // refresh token 생성 (opaque token)
  public String create(Long userId) {

    String token = UUID.randomUUID().toString();
    Duration ttl = Duration.ofSeconds(refreshTokenValiditySeconds);
    redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, String.valueOf(userId), ttl);

    // 다중 로그인 지원
    String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
    redisTemplate.opsForSet().add(userTokensKey, token);
    redisTemplate.expire(userTokensKey, ttl);
    return token;
  }

  // refresh token 조회
  public RefreshLookupResult lookup(String token) {

    String value = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
    if (value == null) return RefreshLookupResult.notFound();

    if (value.startsWith(REVOKED_PREFIX)) {
      Long userId = Long.valueOf(value.substring(REVOKED_PREFIX.length()));
      return RefreshLookupResult.reused(userId);
    }

    return RefreshLookupResult.valid(Long.valueOf(value));
  }

  // refresh token 만료 처리 (REVOKED 상태 5분 유지)
  public void markRotated(String token, Long userId) {
    redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, REVOKED_PREFIX + userId, REVOKED_TOMBSTONE_TTL);
    redisTemplate.opsForSet().remove(USER_TOKENS_KEY_PREFIX + userId, token);
  }

  // refresh token 단건 무효화
  public void revokeOne(String token, Long userId) {

    String tokenKey = TOKEN_KEY_PREFIX + token;
    String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
    redisTemplate.opsForSet().remove(userTokensKey, token);

    // 남은 세션이 있는 경우 만료 처리 & 없는 경우 무효화
    Long remaining = redisTemplate.opsForSet().size(userTokensKey);

    if (remaining != null && remaining > 0L) {
      markRotated(token, userId);
    } else {
      redisTemplate.delete(tokenKey);
      redisTemplate.delete(userTokensKey);
    }
  }

  // 사용자의 모든 refresh token 무효화
  public void revokeAll(Long userId) {

    String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
    Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

    if (tokens != null) {
      tokens.forEach(t -> redisTemplate.delete(TOKEN_KEY_PREFIX + t));
    }

    redisTemplate.delete(userTokensKey);
  }

  // 특정 토큰 외 사용자의 모든 refresh token 무효화
  public void revokeOthers(String token, Long userId) {

    String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
    Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

    if (tokens != null) {
      tokens.stream()
        .filter(t -> !t.equals(token))
        .forEach(t -> {
          redisTemplate.delete(TOKEN_KEY_PREFIX + t);
          redisTemplate.opsForSet().remove(userTokensKey, t);
        });
    }
  }
}
