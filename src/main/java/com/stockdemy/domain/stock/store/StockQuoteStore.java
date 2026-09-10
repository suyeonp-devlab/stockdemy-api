package com.stockdemy.domain.stock.store;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 장중 시세 저장소
 *
 * <p>장중 시세의 원본은 Redis이며, DB {@code stocks}에는 장마감 배치가 하루 한 번 반영한다.
 * 키가 없으면(만료·장애) 조회 측이 DB의 마지막 장마감 값을 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockQuoteStore {

  private static final String KEY_PREFIX = "stock:quote:";
  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  // 시세 저장
  public void save(String stockCode, Snapshot snapshot) {
    redisTemplate.opsForValue().set(KEY_PREFIX + stockCode, objectMapper.writeValueAsString(snapshot), TTL);
  }

  // 시세 다건 조회 (없거나 깨진 값은 제외)
  public Map<String, Snapshot> findAll(Collection<String> stockCodes) {

    Map<String, Snapshot> result = new HashMap<>();

    if (stockCodes.isEmpty()) {
      return result;
    }

    List<String> codes = List.copyOf(stockCodes);
    List<String> values = redisTemplate.opsForValue().multiGet(codes.stream().map(code -> KEY_PREFIX + code).toList());

    if (values == null) {
      return result;
    }

    for (int i = 0; i < codes.size(); i++) {

      Snapshot snapshot = parse(codes.get(i), values.get(i));

      if (snapshot != null) {
        result.put(codes.get(i), snapshot);
      }
    }

    return result;
  }

  private Snapshot parse(String stockCode, String value) {

    if (value == null) return null;

    try {
      return objectMapper.readValue(value, Snapshot.class);
    } catch (Exception e) {
      log.warn("시세 캐시 파싱 실패 (stockCode={}): {}", stockCode, e.getMessage());
      return null;
    }
  }

  @Builder
  public record Snapshot(
    double price,
    double previousClose,
    double changePercent,
    long volume,
    double week52High,
    double week52Low,
    LocalDateTime quotedAt) {
  }
}
