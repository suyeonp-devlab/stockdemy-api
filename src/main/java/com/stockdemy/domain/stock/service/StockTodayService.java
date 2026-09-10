package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.stock.dto.MinuteBarItem;
import com.stockdemy.domain.stock.dto.PriceBarItem;
import com.stockdemy.domain.stock.dto.TodayQuoteResponse;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.store.StockIntradayStore;
import com.stockdemy.domain.stock.store.StockQuoteStore;
import com.stockdemy.domain.stock.support.MarketSession;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import com.stockdemy.global.util.DateTimeUtil;
import com.stockdemy.infra.marketdata.MarketDataProvider;
import com.stockdemy.infra.marketdata.dto.BarItem;
import com.stockdemy.infra.marketdata.dto.IntradayItem;
import com.stockdemy.infra.marketdata.dto.QuoteItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.stockdemy.global.util.NumberUtil.round2;

/**
 * 종목 상세의 당일 시세·분봉
 *
 * <p>상세 화면을 연 종목만 필요한 데이터라 스케줄러가 아니라 조회 시점에 Yahoo 1분봉을 받아
 * 30초 캐시한다. {@code /quote}와 {@code /minute-bars}가 같은 캐시를 쓴다. 외부 호출이 있어
 * 트랜잭션을 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTodayService {

  private final StockRepository stockRepository;
  private final MarketDataProvider marketDataProvider;
  private final StockIntradayStore stockIntradayStore;
  private final StockQuoteStore stockQuoteStore;

  // 당일 분봉 조회
  public List<MinuteBarItem> getMinuteBars(String stockCode) {

    Stock stock = findStock(stockCode);

    return findIntraday(stock)
      .map(intraday -> intraday.bars().stream().map(MinuteBarItem::from).toList())
      .orElse(List.of());
  }

  // 당일 실시간 시세 조회
  public TodayQuoteResponse getTodayQuote(String stockCode) {

    Stock stock = findStock(stockCode);
    MarketSession session = MarketSession.of(stock.getMarket())
      .orElseThrow(() -> new CustomException(ErrorCode.SERVER_ERROR, "장 운영 정보가 없는 시장입니다."));
    boolean marketOpen = session.isOpen(Instant.now());

    return findIntraday(stock)
      .map(intraday -> fromIntraday(intraday, session, marketOpen))
      .orElseGet(() -> fromSnapshot(stock, session, marketOpen));
  }

  // 분봉 기반 당일 시세 (당일 봉은 분봉 집계, 종가는 현재가)
  private TodayQuoteResponse fromIntraday(IntradayItem intraday, MarketSession session, boolean marketOpen) {

    QuoteItem quote = intraday.quote();
    List<BarItem> bars = intraday.bars();
    double price = quote.price();

    if (bars.isEmpty()) {
      LocalDate date = LocalDate.now(session.getZone());
      return build(
        price,
        quote.changePercent(),
        PriceBarItem.of(date, price, price, price, price, quote.volume()),
        MinuteBarItem.placeholder(date, session.getOpen(), price),
        marketOpen,
        quote.quotedAt()
      );
    }

    BarItem latest = bars.getLast();
    double high = Math.max(price, bars.stream().mapToDouble(BarItem::high).max().orElse(price));
    double low = Math.min(price, bars.stream().mapToDouble(BarItem::low).min().orElse(price));

    return build(
      price,
      quote.changePercent(),
      PriceBarItem.of(latest.date(), bars.getFirst().open(), high, low, price, quote.volume()),
      MinuteBarItem.from(latest),
      marketOpen,
      quote.quotedAt()
    );
  }

  // Yahoo 조회 실패 시 장중 시세(Redis) 또는 마지막 장마감 값(DB)으로 대체 (분봉 없이 현재가만)
  private TodayQuoteResponse fromSnapshot(Stock stock, MarketSession session, boolean marketOpen) {

    StockQuoteStore.Snapshot snapshot = findSnapshot(stock.getStockCode());
    Double price = snapshot != null ? Double.valueOf(snapshot.price()) : stock.getLastPrice();

    if (price == null) {
      throw new CustomException(ErrorCode.NOT_FOUND, "시세 정보가 없는 종목입니다.");
    }

    Double changePercent = snapshot != null ? Double.valueOf(snapshot.changePercent()) : stock.getLastChangePercent();
    long volume = snapshot != null ? snapshot.volume() : (stock.getLastVolume() == null ? 0L : stock.getLastVolume());
    LocalDateTime updatedAt = snapshot != null ? snapshot.quotedAt() : stock.getLastSyncedAt();
    LocalDate date = LocalDate.now(session.getZone());

    return build(
      price,
      changePercent,
      PriceBarItem.of(date, price, price, price, price, volume),
      MinuteBarItem.placeholder(date, session.getOpen(), price),
      marketOpen,
      updatedAt
    );
  }

  private TodayQuoteResponse build(
    double price,
    Double changePercent,
    PriceBarItem todayBar,
    MinuteBarItem latestMinuteBar,
    boolean marketOpen,
    LocalDateTime updatedAt
  ) {
    return TodayQuoteResponse.builder()
      .price(round2(price))
      .changePercent(round2(changePercent))
      .todayBar(todayBar)
      .latestMinuteBar(latestMinuteBar)
      .isMarketOpen(marketOpen)
      .updatedAt(DateTimeUtil.toCompactDateTime(updatedAt))
      .build();
  }

  // 캐시 → 없으면 Yahoo 조회 후 캐시
  private Optional<IntradayItem> findIntraday(Stock stock) {

    Optional<IntradayItem> cached = stockIntradayStore.find(stock.getStockCode());

    if (cached.isPresent()) {
      return cached;
    }

    Optional<IntradayItem> fetched = marketDataProvider.fetchIntraday(stock.getStockCode(), stock.getMarket());
    fetched.ifPresent(intraday -> stockIntradayStore.save(stock.getStockCode(), intraday));

    return fetched;
  }

  private StockQuoteStore.Snapshot findSnapshot(String stockCode) {

    try {
      return stockQuoteStore.findAll(List.of(stockCode)).get(stockCode);
    } catch (Exception e) {
      log.warn("시세 캐시 조회 실패, DB 값으로 대체합니다 (stockCode={}): {}", stockCode, e.getMessage());
      return null;
    }
  }

  private Stock findStock(String stockCode) {
    return stockRepository.findById(stockCode)
      .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 종목입니다."));
  }
}
