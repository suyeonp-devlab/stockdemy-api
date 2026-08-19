package com.stockdemy.domain.auth.store;

import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 이메일 인증코드 발급/검증 (Redis) */
@Component
@RequiredArgsConstructor
public class VerificationCodeStore {

  private static final Duration CODE_TTL = Duration.ofMinutes(5);        // 인증코드 유효시간
  private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);   // 인증코드 검증 후 유효시간
  private static final int MAX_VERIFY_ATTEMPTS = 5;                      // 인증코드 최대 시도 횟수
  private static final SecureRandom RANDOM = new SecureRandom();

  private final StringRedisTemplate redisTemplate;

  // 인증코드 발급
  public String issue(VerificationPurpose purpose, String email) {
    String code = String.format("%06d", RANDOM.nextInt(1_000_000));
    redisTemplate.opsForValue().set(codeKey(purpose, email), code, CODE_TTL);
    redisTemplate.delete(attemptsKey(purpose, email)); // 시도횟수 초기화
    return code;
  }

  // 인증코드 검증
  public void verify(VerificationPurpose purpose, String email, String code) {
    String saved = redisTemplate.opsForValue().get(codeKey(purpose, email));
    if (saved == null) throw new CustomException(ErrorCode.CODE_EXPIRED);

    if (!saved.equals(code)) {
      long attempts = registerFailedAttempt(purpose, email);
      if (attempts >= MAX_VERIFY_ATTEMPTS) {
        redisTemplate.delete(codeKey(purpose, email));
        redisTemplate.delete(attemptsKey(purpose, email));
        throw new CustomException(ErrorCode.CODE_EXPIRED, "인증 시도 횟수를 초과했습니다. 인증코드를 다시 요청해주세요.");
      }
      throw new CustomException(ErrorCode.CODE_MISMATCH);
    }

    redisTemplate.delete(codeKey(purpose, email));
    redisTemplate.delete(attemptsKey(purpose, email));
    redisTemplate.opsForValue().set(verifiedKey(purpose, email), "true", VERIFIED_TTL);
  }

  // 인증코드 검증 완료 여부
  public void requireVerified(VerificationPurpose purpose, String email) {
    Boolean deleted = redisTemplate.delete(verifiedKey(purpose, email));
    if (Boolean.FALSE.equals(deleted)) {
      throw new CustomException(ErrorCode.CODE_NOT_VERIFIED);
    }
  }

  // 인증코드 시도 횟수
  private long registerFailedAttempt(VerificationPurpose purpose, String email) {
    String key = attemptsKey(purpose, email);
    Long attempts = redisTemplate.opsForValue().increment(key);
    if (attempts != null && attempts == 1L) {
      redisTemplate.expire(key, CODE_TTL);
    }
    return attempts == null ? 1L : attempts;
  }

  // 인증코드 요청 key
  private String codeKey(VerificationPurpose purpose, String email) {
    return "verify-code:" + purpose + ":" + email;
  }

  // 인증코드 검증완료 key
  private String verifiedKey(VerificationPurpose purpose, String email) {
    return "verify-ok:" + purpose + ":" + email;
  }

  // 인증코드 시도횟수 key
  private String attemptsKey(VerificationPurpose purpose, String email) {
    return "verify-attempts:" + purpose + ":" + email;
  }
}
