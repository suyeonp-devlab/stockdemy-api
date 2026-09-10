package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.stock.dto.FavoriteRequest;
import com.stockdemy.domain.stock.dto.PriceBarItem;
import com.stockdemy.domain.stock.dto.StockFundamentalsResponse;
import com.stockdemy.domain.stock.dto.StockItem;
import com.stockdemy.domain.stock.dto.StockListResponse;
import com.stockdemy.domain.stock.dto.StockQuoteItem;
import com.stockdemy.domain.stock.dto.StockSearchRequest;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.PriceBarRepository;
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

import static com.stockdemy.global.util.NumberUtil.round2;

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
  private final PriceBarRepository priceBarRepository;
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

  // 종목 기초데이터 조회
  public StockFundamentalsResponse getFundamentals(String stockCode, Long userId) {

    Stock stock = stockRepository.findById(stockCode)
      .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 종목입니다."));

    boolean favorite = userId != null && userFavoriteStockRepository.existsByUserIdAndStockCode(userId, stockCode);

    return StockFundamentalsResponse.builder()
      .stockCode(stock.getStockCode())
      .stockName(stock.getStockName())
      .market(stock.getMarket())
      .marketNm(codeService.getCodeName(MARKET_GROUP, stock.getMarket()))
      .sector(stock.getSector())
      .sectorNm(codeService.getCodeName(SECTOR_GROUP, stock.getSector()))
      .sentiment(stock.getSentiment())
      .sentimentNm(codeService.getCodeName(SENTIMENT_GROUP, stock.getSentiment()))
      .favorite(favorite)
      .aiComment(stock.getAiComment())
      .prevClose(round2(stock.getPrevClose()))
      .week52High(round2(stock.getWeek52High()))
      .week52Low(round2(stock.getWeek52Low()))
      .sharesOutstanding(stock.getSharesOutstanding())
      .foreignOwnership(stock.getForeignOwnership())
      .eps(stock.getEps())
      .bps(stock.getBps())
      .annualDividend(stock.getAnnualDividend())
      .sectorPer(stock.getSectorPer())
      .build();
  }

  // 일봉 조회 (DB에 적재된 확정 봉, 당일 진행 중인 봉은 실시간 시세 API가 제공)
  public List<PriceBarItem> getPriceBars(String stockCode) {

    if (!stockRepository.existsById(stockCode)) {
      throw new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 종목입니다.");
    }

    return priceBarRepository.findByStockCodeOrderByBarDateAsc(stockCode).stream()
      .map(PriceBarItem::from)
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

  private record LiveQuote(Double price, Double changePercent, long volume) {

    double tradingValue() {
      return price == null ? 0 : price * volume;
    }
  }
}
