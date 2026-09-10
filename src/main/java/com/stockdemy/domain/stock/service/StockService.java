package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.stock.dto.FavoriteRequest;
import com.stockdemy.domain.stock.dto.StockItem;
import com.stockdemy.domain.stock.dto.StockListResponse;
import com.stockdemy.domain.stock.dto.StockQuoteItem;
import com.stockdemy.domain.stock.dto.StockSearchRequest;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockQueryRepository;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.repository.UserFavoriteStockRepository;
import com.stockdemy.domain.stock.store.StockQuoteStore;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

  private static final String MARKET_GROUP = "STOCK_MARKET";
  private static final String SECTOR_GROUP = "STOCK_SECTOR";
  private static final String SENTIMENT_GROUP = "AI_SENTIMENT";
  private static final int MAX_QUOTE_CODES = 100;

  private final StockRepository stockRepository;
  private final StockQueryRepository stockQueryRepository;
  private final UserFavoriteStockRepository userFavoriteStockRepository;
  private final StockQuoteStore stockQuoteStore;
  private final CodeService codeService;

  // 종목 목록 조회
  public StockListResponse getStockList(StockSearchRequest request, Long userId) {

    if (request.favorite() && userId == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    List<Stock> stocks = stockQueryRepository.search(
      request.market(),
      request.sector(),
      request.keyword(),
      request.favorite() ? userId : null
    );

    Map<String, LiveQuote> quotes = resolveLiveQuotes(stocks);

    // 거래량 상위 탭은 거래량순, 그 외는 시가총액 대신 거래대금(현재가 × 거래량)순
    Comparator<Stock> order = request.topVolume()
      ? Comparator.comparingDouble(stock -> quotes.get(stock.getStockCode()).volume())
      : Comparator.comparingDouble(stock -> quotes.get(stock.getStockCode()).tradingValue());

    List<Stock> sorted = stocks.stream()
      .sorted(order.reversed().thenComparing(Stock::getStockCode))
      .toList();

    int totalCount = sorted.size();
    int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / request.pageSize()));
    int from = Math.min((request.page() - 1) * request.pageSize(), totalCount);
    int to = Math.min(from + request.pageSize(), totalCount);

    Set<String> favoriteCodes = userId == null
      ? Set.of()
      : new HashSet<>(userFavoriteStockRepository.findStockCodesByUserId(userId));

    Map<String, String> marketNames = codeService.getCodeNameMap(MARKET_GROUP);
    Map<String, String> sectorNames = codeService.getCodeNameMap(SECTOR_GROUP);
    Map<String, String> sentimentNames = codeService.getCodeNameMap(SENTIMENT_GROUP);

    List<StockItem> items = sorted.subList(from, to).stream()
      .map(stock -> {
        LiveQuote quote = quotes.get(stock.getStockCode());
        return StockItem.builder()
          .stockCode(stock.getStockCode())
          .stockName(stock.getStockName())
          .market(stock.getMarket())
          .marketNm(marketNames.get(stock.getMarket()))
          .sector(stock.getSector())
          .sectorNm(sectorNames.get(stock.getSector()))
          .price(round2(quote.price()))
          .changePercent(round2(quote.changePercent()))
          .marketCap(stock.getLastMarketCap())
          .sentiment(stock.getSentiment())
          .sentimentNm(sentimentNames.get(stock.getSentiment()))
          .favorite(favoriteCodes.contains(stock.getStockCode()))
          .build();
      })
      .toList();

    return new StockListResponse(totalCount, totalPages, items);
  }

  // 종목 실시간 시세 조회
  public List<StockQuoteItem> getStockQuotes(List<String> stockCodes) {

    List<String> codes = stockCodes == null ? List.of() : stockCodes.stream()
      .filter(StringUtils::hasText)
      .map(String::trim)
      .distinct()
      .toList();

    if (codes.isEmpty()) {
      return List.of();
    }

    if (codes.size() > MAX_QUOTE_CODES) {
      throw new CustomException(ErrorCode.INVALID_REQUEST, "한 번에 조회할 수 있는 종목은 최대 " + MAX_QUOTE_CODES + "개입니다.");
    }

    List<Stock> stocks = stockRepository.findAllById(codes);
    Map<String, LiveQuote> quotes = resolveLiveQuotes(stocks);

    return stocks.stream()
      .map(stock -> {
        LiveQuote quote = quotes.get(stock.getStockCode());
        return StockQuoteItem.builder()
          .stockCode(stock.getStockCode())
          .price(round2(quote.price()))
          .changePercent(round2(quote.changePercent()))
          .marketCap(stock.getLastMarketCap())
          .build();
      })
      .toList();
  }

  // 관심종목 등록/해제 (이미 같은 상태면 그대로 성공)
  @Transactional
  public void updateFavorite(Long userId, FavoriteRequest request) {

    String stockCode = request.stockCode().trim();

    if (!stockRepository.existsById(stockCode)) {
      throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 종목입니다.");
    }

    if (request.favorite()) {
      userFavoriteStockRepository.insertIfAbsent(userId, stockCode, LocalDateTime.now());
    } else {
      userFavoriteStockRepository.deleteByUserIdAndStockCode(userId, stockCode);
    }
  }

  // 실시간 시세 결합 (Redis 우선, 값이 없거나 Redis 장애 시 DB의 마지막 장마감 값)
  private Map<String, LiveQuote> resolveLiveQuotes(List<Stock> stocks) {

    Map<String, StockQuoteStore.Snapshot> snapshots = findSnapshots(stocks.stream().map(Stock::getStockCode).toList());
    Map<String, LiveQuote> result = new HashMap<>();

    for (Stock stock : stocks) {

      StockQuoteStore.Snapshot snapshot = snapshots.get(stock.getStockCode());

      LiveQuote quote = snapshot != null
        ? new LiveQuote(snapshot.price(), snapshot.changePercent(), snapshot.volume())
        : new LiveQuote(stock.getLastPrice(), stock.getLastChangePercent(), stock.getLastVolume() == null ? 0L : stock.getLastVolume());

      result.put(stock.getStockCode(), quote);
    }

    return result;
  }

  private Map<String, StockQuoteStore.Snapshot> findSnapshots(List<String> stockCodes) {

    try {
      return stockQuoteStore.findAll(stockCodes);
    } catch (Exception e) {
      log.warn("시세 캐시 조회 실패, DB 값으로 대체합니다: {}", e.getMessage());
      return Map.of();
    }
  }

  // 앱이 등락률을 받은 그대로 출력하므로 소수 둘째 자리로 맞춘다
  private Double round2(Double value) {
    return value == null ? null : Math.round(value * 100) / 100.0;
  }

  private record LiveQuote(Double price, Double changePercent, long volume) {

    double tradingValue() {
      return price == null ? 0 : price * volume;
    }
  }
}
