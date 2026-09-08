package com.stockdemy.infra.marketdata;

import com.stockdemy.infra.marketdata.dto.BarItem;
import com.stockdemy.infra.marketdata.dto.IndexItem;
import com.stockdemy.infra.marketdata.dto.QuoteItem;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * local 개발용 시세 프로바이더
 *
 * <p>외부 호출 없이 종목코드 기반 결정적 난수로 시세를 만든다. 같은 종목은 항상 같은 기준가를
 * 갖고, 장중에는 3초 단위로 값이 바뀌어 폴링 화면이 실제처럼 보인다.
 */
@Slf4j
public class DummyMarketDataProvider implements MarketDataProvider {

  private static final String MARKET_NASDAQ = "NASDAQ";
  private static final LocalTime KRX_OPEN = LocalTime.of(9, 0);
  private static final LocalTime KRX_CLOSE = LocalTime.of(15, 30);
  private static final LocalTime US_OPEN = LocalTime.of(9, 30);
  private static final LocalTime US_CLOSE = LocalTime.of(16, 0);
  private static final int TICK_SECONDS = 3;

  // 현재가 조회
  @Override
  public Optional<QuoteItem> fetchQuote(String stockCode, String market) {

    double previousClose = basePrice(stockCode, market);
    double price = round(previousClose * (1 + tickDrift(stockCode)), market);
    Random random = seeded(stockCode, 4);

    return Optional.of(QuoteItem.builder()
      .stockCode(stockCode)
      .price(price)
      .previousClose(previousClose)
      .changePercent(round2((price - previousClose) / previousClose * 100))
      .volume(baseVolume(stockCode))
      .week52High(round(previousClose * (1.15 + random.nextDouble() * 0.35), market))
      .week52Low(round(previousClose * (0.6 + random.nextDouble() * 0.3), market))
      .marketOpen(isMarketOpen(market))
      .quotedAt(LocalDateTime.now())
      .build());
  }

  // 일봉 조회
  @Override
  public List<BarItem> fetchDailyBars(String stockCode, String market, int days) {

    List<BarItem> bars = new ArrayList<>();
    Random random = seeded(stockCode, 1);
    double close = basePrice(stockCode, market);
    LocalDate date = lastTradingDate().minusDays(days);

    while (!date.isAfter(lastTradingDate())) {

      if (isWeekend(date)) {
        date = date.plusDays(1);
        continue;
      }

      close = close * (1 + (random.nextDouble() - 0.5) * 0.04);
      bars.add(buildBar(date, null, close, random, market, stockCode));
      date = date.plusDays(1);
    }

    return bars;
  }

  // 당일 분봉 조회
  @Override
  public List<BarItem> fetchMinuteBars(String stockCode, String market) {

    LocalDate date = lastTradingDate();
    LocalTime open = MARKET_NASDAQ.equals(market) ? US_OPEN : KRX_OPEN;
    LocalTime close = MARKET_NASDAQ.equals(market) ? US_CLOSE : KRX_CLOSE;
    LocalTime until = date.equals(LocalDate.now()) && LocalTime.now().isBefore(close) ? LocalTime.now() : close;

    if (until.isBefore(open)) {
      return List.of();
    }

    List<BarItem> bars = new ArrayList<>();
    Random random = seeded(stockCode, 2);
    double price = basePrice(stockCode, market);

    for (LocalTime at = open; !at.isAfter(until); at = at.plusMinutes(1)) {
      price = price * (1 + (random.nextDouble() - 0.5) * 0.002);
      bars.add(buildBar(date, at, price, random, market, stockCode));
    }

    return bars;
  }

  // 지수·환율 조회
  @Override
  public List<IndexItem> fetchIndices() {

    List<IndexItem> items = new ArrayList<>();

    for (MarketIndexType type : MarketIndexType.values()) {

      double base = type.getDummyBaseValue();
      double value = round2(base * (1 + tickDrift(type.getMarketCode())));

      items.add(IndexItem.builder()
        .marketCode(type.getMarketCode())
        .marketName(type.getMarketName())
        .indexValue(value)
        .changePercent(round2((value - base) / base * 100))
        .build());
    }

    return items;
  }

  // 봉 생성 (종가 기준으로 시/고/저 파생)
  private BarItem buildBar(LocalDate date, LocalTime time, double close, Random random, String market, String stockCode) {

    double open = close * (1 + (random.nextDouble() - 0.5) * 0.01);
    double high = Math.max(open, close) * (1 + random.nextDouble() * 0.005);
    double low = Math.min(open, close) * (1 - random.nextDouble() * 0.005);
    long volume = baseVolume(stockCode) / (time == null ? 1 : 390);
    double roundedClose = round(close, market);

    return BarItem.builder()
      .date(date)
      .time(time)
      .open(round(open, market))
      .high(round(high, market))
      .low(round(low, market))
      .close(roundedClose)
      .volume(volume)
      .tradingValue(Math.round(roundedClose * volume))
      .build();
  }

  // 종목별 고정 기준가
  private double basePrice(String stockCode, String market) {

    Random random = seeded(stockCode, 0);

    if (MARKET_NASDAQ.equals(market)) {
      return round2(50 + random.nextDouble() * 850);
    }

    return Math.round((10_000 + random.nextDouble() * 290_000) / 100) * 100.0;
  }

  // 종목별 고정 거래량
  private long baseVolume(String stockCode) {
    return 100_000 + (long) (seeded(stockCode, 3).nextDouble() * 4_900_000);
  }

  // 3초 단위로 변하는 등락률 (폴링 시 값이 실제로 움직이도록)
  private double tickDrift(String key) {
    long bucket = Instant.now().getEpochSecond() / TICK_SECONDS;
    return (seeded(key, bucket).nextDouble() - 0.5) * 0.03;
  }

  // 정규장 운영 여부 (NASDAQ도 서버 로컬시간 기준으로 판정하는 개발용 근사)
  private boolean isMarketOpen(String market) {

    if (isWeekend(LocalDate.now())) return false;

    LocalTime now = LocalTime.now();
    LocalTime open = MARKET_NASDAQ.equals(market) ? US_OPEN : KRX_OPEN;
    LocalTime close = MARKET_NASDAQ.equals(market) ? US_CLOSE : KRX_CLOSE;

    return !now.isBefore(open) && now.isBefore(close);
  }

  // 주말이면 직전 금요일
  private LocalDate lastTradingDate() {

    LocalDate date = LocalDate.now();

    while (isWeekend(date)) {
      date = date.minusDays(1);
    }

    return date;
  }

  private boolean isWeekend(LocalDate date) {
    return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
  }

  private Random seeded(String key, long salt) {
    return new Random(key.hashCode() * 31L + salt);
  }

  private double round(double value, String market) {
    return MARKET_NASDAQ.equals(market) ? round2(value) : Math.round(value / 10) * 10.0;
  }

  private double round2(double value) {
    return Math.round(value * 100) / 100.0;
  }
}
