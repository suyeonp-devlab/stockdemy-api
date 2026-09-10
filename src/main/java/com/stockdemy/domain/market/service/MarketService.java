package com.stockdemy.domain.market.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.market.dto.MarketIndexItem;
import com.stockdemy.domain.market.dto.SectorSummaryItem;
import com.stockdemy.domain.market.store.MarketIndexStore;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.store.StockQuoteStore;
import com.stockdemy.infra.marketdata.MarketDataProvider;
import com.stockdemy.infra.marketdata.MarketIndexType;
import com.stockdemy.infra.marketdata.dto.IndexItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.stockdemy.global.util.NumberUtil.round2;

/**
 * 시장 지수·업종 요약
 *
 * <p>지수는 스케줄러가 Redis에 갱신한 값만 읽어 조회 요청이 외부 호출을 기다리지 않는다.
 * 업종 등락률은 종목 시세(Redis 우선, 없으면 DB의 마지막 장마감 값)를 업종별로 집계한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

  private static final String SECTOR_GROUP = "STOCK_SECTOR";

  // 한국 장과 미국 장은 움직이는 시간대가 달라 업종 집계는 KOSPI 종목만 사용
  private static final String SECTOR_MARKET = "KOSPI";

  // 대시보드 업종 카드 표시 개수 (좌우 7개씩)
  private static final int SECTOR_LIMIT = 14;

  private final MarketDataProvider marketDataProvider;
  private final MarketIndexStore marketIndexStore;
  private final StockRepository stockRepository;
  private final StockQuoteStore stockQuoteStore;
  private final CodeService codeService;

  // 지수·환율 조회 (갱신 전이거나 Redis 장애면 빈 목록 → 앱이 티커를 숨김)
  public List<MarketIndexItem> getIndices() {
    return marketIndexStore.findAll().stream()
      .map(MarketIndexItem::from)
      .toList();
  }

  // 지수·환율 갱신 (일부 지수만 실패하면 해당 지수는 이전 값 유지)
  public void syncIndices() {

    List<IndexItem> fetched = marketDataProvider.fetchIndices();

    if (fetched.isEmpty()) {
      log.warn("지수 갱신 실패: 이전 값을 유지합니다.");
      return;
    }

    Map<String, IndexItem> fetchedByCode = toCodeMap(fetched);
    Map<String, IndexItem> previousByCode = toCodeMap(marketIndexStore.findAll());
    List<IndexItem> merged = new ArrayList<>();

    for (MarketIndexType type : MarketIndexType.values()) {

      IndexItem item = fetchedByCode.getOrDefault(type.getMarketCode(), previousByCode.get(type.getMarketCode()));

      if (item != null) {
        merged.add(item);
      }
    }

    marketIndexStore.saveAll(merged);
    log.debug("지수 갱신: {}/{}건", fetched.size(), MarketIndexType.values().length);
  }

  // 업종별 등락 요약 (업종 내 종목 등락률 단순 평균, 등락률 내림차순)
  @Transactional(readOnly = true)
  public List<SectorSummaryItem> getSectorSummaries() {

    List<Stock> stocks = stockRepository.findByMarket(SECTOR_MARKET);
    Map<String, StockQuoteStore.Snapshot> snapshots = findSnapshots(stocks.stream().map(Stock::getStockCode).toList());
    Map<String, List<Double>> changesBySector = new HashMap<>();

    for (Stock stock : stocks) {

      StockQuoteStore.Snapshot snapshot = snapshots.get(stock.getStockCode());
      Double changePercent = snapshot != null ? Double.valueOf(snapshot.changePercent()) : stock.getLastChangePercent();

      // 시세를 한 번도 받지 못한 종목은 평균에서 제외
      if (changePercent == null) continue;

      changesBySector.computeIfAbsent(stock.getSector(), sector -> new ArrayList<>()).add(changePercent);
    }

    Map<String, String> sectorNames = codeService.getCodeNameMap(SECTOR_GROUP);

    // 반올림한 값으로 정렬해 화면 순위와 표시 값이 어긋나지 않게 한다 (동률은 업종코드순)
    return changesBySector.entrySet().stream()
      .filter(entry -> sectorNames.containsKey(entry.getKey()))
      .map(entry -> SectorSummaryItem.builder()
        .sector(entry.getKey())
        .sectorNm(sectorNames.get(entry.getKey()))
        .changePercent(round2(entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)))
        .build())
      .sorted(Comparator.comparing(SectorSummaryItem::changePercent).reversed()
        .thenComparing(SectorSummaryItem::sector))
      .limit(SECTOR_LIMIT)
      .toList();
  }

  private Map<String, IndexItem> toCodeMap(List<IndexItem> items) {
    return items.stream().collect(Collectors.toMap(IndexItem::marketCode, Function.identity(), (first, second) -> first));
  }

  private Map<String, StockQuoteStore.Snapshot> findSnapshots(List<String> stockCodes) {

    try {
      return stockQuoteStore.findAll(stockCodes);
    } catch (Exception e) {
      log.warn("시세 캐시 조회 실패, DB 값으로 대체합니다: {}", e.getMessage());
      return Map.of();
    }
  }
}
