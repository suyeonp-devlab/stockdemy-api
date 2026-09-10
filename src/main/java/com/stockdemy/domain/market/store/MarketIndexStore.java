package com.stockdemy.domain.market.store;

import com.stockdemy.infra.marketdata.dto.IndexItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

/**
 * 지수·환율 저장소
 *
 * <p>스케줄러가 갱신하고 조회 API는 여기서만 읽는다. 갱신이 멈춰도 마지막 값을 보여주도록 TTL을 길게 둔다.
 * 읽기·쓰기 실패는 예외를 던지지 않고 빈 값으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketIndexStore {

  private static final String KEY = "market:indices";
  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  // 지수 목록 조회
  public List<IndexItem> findAll() {

    try {
      String value = redisTemplate.opsForValue().get(KEY);
      return value == null ? List.of() : List.of(objectMapper.readValue(value, IndexItem[].class));
    } catch (Exception e) {
      log.warn("지수 캐시 조회 실패: {}", e.getMessage());
      return List.of();
    }
  }

  // 지수 목록 저장
  public void saveAll(List<IndexItem> items) {

    try {
      redisTemplate.opsForValue().set(KEY, objectMapper.writeValueAsString(items), TTL);
    } catch (Exception e) {
      log.warn("지수 캐시 저장 실패: {}", e.getMessage());
    }
  }
}
