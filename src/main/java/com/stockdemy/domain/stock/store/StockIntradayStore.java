package com.stockdemy.domain.stock.store;

import com.stockdemy.infra.marketdata.dto.IntradayItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * 당일 시세·분봉 캐시
 *
 * <p>상세 화면의 5초 폴링을 흡수해 종목당 Yahoo 호출을 30초에 한 번으로 줄인다. 캐시일 뿐이라
 * Redis 장애나 형식 변경으로 읽기·쓰기에 실패해도 예외를 던지지 않고 캐시 미스로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockIntradayStore {

  private static final String KEY_PREFIX = "stock:intraday:";
  private static final Duration TTL = Duration.ofSeconds(30);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  // 캐시 조회
  public Optional<IntradayItem> find(String stockCode) {

    try {
      String value = redisTemplate.opsForValue().get(KEY_PREFIX + stockCode);
      return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, IntradayItem.class));
    } catch (Exception e) {
      log.warn("당일 시세 캐시 조회 실패 (stockCode={}): {}", stockCode, e.getMessage());
      return Optional.empty();
    }
  }

  // 캐시 저장
  public void save(String stockCode, IntradayItem intraday) {

    try {
      redisTemplate.opsForValue().set(KEY_PREFIX + stockCode, objectMapper.writeValueAsString(intraday), TTL);
    } catch (Exception e) {
      log.warn("당일 시세 캐시 저장 실패 (stockCode={}): {}", stockCode, e.getMessage());
    }
  }
}
