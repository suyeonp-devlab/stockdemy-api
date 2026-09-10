package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.stock.dto.StockDisclosureItem;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.infra.dart.DisclosureProvider;
import com.stockdemy.infra.dart.dto.DisclosureItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종목 공시
 *
 * <p>DART 유가증권시장 최근 공시에서 추적 종목만 거른다. DART에서 종목별로 정확히 조회하려면
 * {@code corp_code} 매핑이 필요해 이후 단계에서 다룬다. 외부 호출이 있어 트랜잭션을 두지 않는다.
 */
@Service
@RequiredArgsConstructor
public class StockDisclosureService {

  // 화면 조회마다 DART를 부르지 않도록 짧게 메모리 캐시 (서버 1대 가정)
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  // 종목 검색 화면의 오늘의 공시 카드 표시 건수
  private static final int MARKET_LIMIT = 10;

  private final DisclosureProvider disclosureProvider;
  private final StockRepository stockRepository;

  private volatile CachedDisclosures cache;

  // 공시 조회 (종목코드가 없으면 추적 종목 전체의 최근 공시)
  public List<StockDisclosureItem> getDisclosures(String stockCode) {

    List<DisclosureItem> recent = findRecent();

    // 종목코드 → 종목명 (추적 종목 판별과 표시 이름에 함께 사용)
    Map<String, String> stockNames = stockRepository.findAll().stream()
      .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));

    if (StringUtils.hasText(stockCode)) {
      String code = stockCode.trim();
      return recent.stream()
        .filter(item -> code.equals(item.stockCode()))
        .map(item -> StockDisclosureItem.from(item, stockNames.get(item.stockCode())))
        .toList();
    }

    return recent.stream()
      .filter(item -> stockNames.containsKey(item.stockCode()))
      .limit(MARKET_LIMIT)
      .map(item -> StockDisclosureItem.from(item, stockNames.get(item.stockCode())))
      .toList();
  }

  // 최근 공시 (접수번호 역순 = 최신순)
  private List<DisclosureItem> findRecent() {

    CachedDisclosures current = cache;

    if (current != null && current.fetchedAt().plus(CACHE_TTL).isAfter(Instant.now())) {
      return current.items();
    }

    List<DisclosureItem> items = disclosureProvider.fetchRecent().stream()
      .sorted(Comparator.comparing(DisclosureItem::receiptNo, Comparator.nullsLast(Comparator.reverseOrder())))
      .toList();

    // 조회 실패(빈 목록)는 캐시하지 않고 다음 요청에서 다시 시도한다
    if (!items.isEmpty()) {
      cache = new CachedDisclosures(items, Instant.now());
    }

    return items;
  }

  private record CachedDisclosures(List<DisclosureItem> items, Instant fetchedAt) {
  }
}
