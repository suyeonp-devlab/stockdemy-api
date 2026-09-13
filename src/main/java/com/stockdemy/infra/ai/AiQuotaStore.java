package com.stockdemy.infra.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * AI 일일 호출량 저장소
 *
 * <p>용도별 당일 호출 수를 센다. 재기동해도 당일 사용량이 유지되도록 Redis에 둔다. Redis 장애 시에는
 * 제한을 걸지 않고 통과시킨다(한도 초과는 429 응답으로 다시 걸러진다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiQuotaStore {

  private static final String KEY_PREFIX = "ai:quota:";
  private static final DateTimeFormatter KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final Duration TTL = Duration.ofDays(2);

  private final StringRedisTemplate redisTemplate;

  // 당일 호출 수
  public long count(AiUsage usage) {

    try {
      String value = redisTemplate.opsForValue().get(key(usage));
      return value == null ? 0 : Long.parseLong(value);
    } catch (Exception e) {
      log.warn("AI 호출량 조회 실패 ({}): {}", usage, e.getMessage());
      return 0;
    }
  }

  // 당일 호출 수 증가
  public void increase(AiUsage usage) {

    try {
      String key = key(usage);
      Long count = redisTemplate.opsForValue().increment(key);

      // 날짜가 바뀌어 새로 만들어진 키에만 만료를 건다
      if (count != null && count == 1L) {
        redisTemplate.expire(key, TTL);
      }

    } catch (Exception e) {
      log.warn("AI 호출량 기록 실패 ({}): {}", usage, e.getMessage());
    }
  }

  // ai:quota:{yyyyMMdd}:{용도}
  private String key(AiUsage usage) {
    return KEY_PREFIX + LocalDate.now().format(KEY_DATE_FORMAT) + ":" + usage.name().toLowerCase();
  }
}
