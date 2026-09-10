package com.stockdemy.infra.marketdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.stockdemy.infra.marketdata.dto.BarItem;
import com.stockdemy.infra.marketdata.dto.IndexItem;
import com.stockdemy.infra.marketdata.dto.IntradayItem;
import com.stockdemy.infra.marketdata.dto.QuoteItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class YahooMarketDataProvider implements MarketDataProvider {

  private static final String BASE_URL = "https://query1.finance.yahoo.com";
  private static final String CHART_PATH = "/v8/finance/chart/{symbol}";

  // 기본 UA로는 Yahoo가 응답을 거부한다
  private static final String USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36";

  private static final String INTERVAL_DAILY = "1d";
  private static final String INTERVAL_MINUTE = "1m";

  // chartPreviousClose는 조회 구간 시작 직전 종가라서, 전일 종가가 되려면 구간이 최근 1거래일이어야 한다
  private static final String RANGE_LATEST_SESSION = "1d";

  private final RestClient restClient = RestClient.builder()
    .baseUrl(BASE_URL)
    .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
    .build();

  // 현재가 조회
  @Override
  public Optional<QuoteItem> fetchQuote(String stockCode, String market) {

    ChartResult result = fetchChart(toSymbol(stockCode, market), INTERVAL_DAILY, RANGE_LATEST_SESSION, null, null);

    return toQuote(stockCode, result);
  }

  // 일봉 조회
  @Override
  public List<BarItem> fetchDailyBars(String stockCode, String market, int days) {

    long end = Instant.now().getEpochSecond();
    long start = Instant.now().minusSeconds((long) days * 86_400).getEpochSecond();

    ChartResult result = fetchChart(toSymbol(stockCode, market), INTERVAL_DAILY, null, start, end);

    return toBars(result, false);
  }

  // 당일 시세 + 분봉 조회
  @Override
  public Optional<IntradayItem> fetchIntraday(String stockCode, String market) {

    ChartResult result = fetchChart(toSymbol(stockCode, market), INTERVAL_MINUTE, RANGE_LATEST_SESSION, null, null);

    return toQuote(stockCode, result).map(quote -> new IntradayItem(quote, toBars(result, true)));
  }

  // 지수·환율 조회
  @Override
  public List<IndexItem> fetchIndices() {

    List<IndexItem> items = new ArrayList<>();

    for (MarketIndexType type : MarketIndexType.values()) {

      ChartResult result = fetchChart(type.getYahooSymbol(), INTERVAL_DAILY, RANGE_LATEST_SESSION, null, null);

      if (result == null || result.meta() == null || result.meta().regularMarketPrice() == null) {
        continue;
      }

      double indexValue = result.meta().regularMarketPrice();
      double previousClose = resolvePreviousClose(result.meta(), indexValue);

      items.add(IndexItem.builder()
        .marketCode(type.getMarketCode())
        .marketName(type.getMarketName())
        .indexValue(indexValue)
        .changePercent(resolveChangePercent(result.meta(), indexValue, previousClose))
        .build());
    }

    return items;
  }

  // 종목코드 → Yahoo 심볼 (NASDAQ은 접미사 없음)
  private String toSymbol(String stockCode, String market) {

    return switch (market) {
      case "KOSPI" -> stockCode + ".KS";
      case "KOSDAQ" -> stockCode + ".KQ";
      default -> stockCode;
    };
  }

  // chart API 호출
  private ChartResult fetchChart(String symbol, String interval, String range, Long period1, Long period2) {

    try {
      UriComponentsBuilder uri = UriComponentsBuilder.fromPath(CHART_PATH).queryParam("interval", interval);

      if (range != null) {
        uri.queryParam("range", range);
      } else {
        uri.queryParam("period1", period1).queryParam("period2", period2);
      }

      // 지수 심볼(^KS11)과 환율 심볼(KRW=X)에 URI 예약문자가 들어가므로 확장 후 인코딩한다
      ChartResponse response = restClient.get()
        .uri(uri.buildAndExpand(symbol).encode().toUriString())
        .retrieve()
        .body(ChartResponse.class);

      if (response == null || response.chart() == null || response.chart().result() == null
        || response.chart().result().isEmpty()) {
        return null;
      }

      return response.chart().result().getFirst();

    } catch (Exception e) {
      log.warn("Yahoo 시세 조회 실패 (symbol={}): {}", symbol, e.getMessage());
      return null;
    }
  }

  // 응답 meta → 현재가
  private Optional<QuoteItem> toQuote(String stockCode, ChartResult result) {

    if (result == null || result.meta() == null || result.meta().regularMarketPrice() == null) {
      return Optional.empty();
    }

    ChartMeta meta = result.meta();
    double price = meta.regularMarketPrice();
    double previousClose = resolvePreviousClose(meta, price);

    return Optional.of(QuoteItem.builder()
      .stockCode(stockCode)
      .price(price)
      .previousClose(previousClose)
      .changePercent(resolveChangePercent(meta, price, previousClose))
      .volume(meta.regularMarketVolume() == null ? 0L : meta.regularMarketVolume())
      .week52High(meta.fiftyTwoWeekHigh() == null ? 0 : meta.fiftyTwoWeekHigh())
      .week52Low(meta.fiftyTwoWeekLow() == null ? 0 : meta.fiftyTwoWeekLow())
      .quotedAt(LocalDateTime.now())
      .build());
  }

  // 봉 배열 변환
  private List<BarItem> toBars(ChartResult result, boolean withTime) {

    if (result == null || result.timestamp() == null || result.indicators() == null
      || result.indicators().quote() == null || result.indicators().quote().isEmpty()) {
      return List.of();
    }

    ChartQuote quote = result.indicators().quote().getFirst();
    ZoneId zone = resolveZone(result.meta());
    List<BarItem> bars = new ArrayList<>();

    for (int i = 0; i < result.timestamp().size(); i++) {

      Double open = valueAt(quote.open(), i);
      Double high = valueAt(quote.high(), i);
      Double low = valueAt(quote.low(), i);
      Double close = valueAt(quote.close(), i);

      // 장중 결측 구간은 null로 내려온다
      if (open == null || high == null || low == null || close == null) continue;

      Long volume = valueAt(quote.volume(), i);
      long resolvedVolume = volume == null ? 0L : volume;
      ZonedDateTime at = Instant.ofEpochSecond(result.timestamp().get(i)).atZone(zone);

      bars.add(BarItem.builder()
        .date(at.toLocalDate())
        .time(withTime ? at.toLocalTime().withSecond(0).withNano(0) : null)
        .open(open)
        .high(high)
        .low(low)
        .close(close)
        .volume(resolvedVolume)
        // Yahoo는 거래대금을 주지 않아 종가 × 거래량으로 근사한다
        .tradingValue(Math.round(close * resolvedVolume))
        .build());
    }

    return bars;
  }

  private ZoneId resolveZone(ChartMeta meta) {

    if (meta == null || meta.exchangeTimezoneName() == null) {
      return ZoneId.systemDefault();
    }

    try {
      return ZoneId.of(meta.exchangeTimezoneName());
    } catch (Exception e) {
      return ZoneId.systemDefault();
    }
  }

  private double resolvePreviousClose(ChartMeta meta, double fallback) {

    if (meta.chartPreviousClose() != null) return meta.chartPreviousClose();
    if (meta.previousClose() != null) return meta.previousClose();
    return fallback;
  }

  // 등락률은 Yahoo 계산값을 우선 사용한다
  private double resolveChangePercent(ChartMeta meta, double price, double previousClose) {

    if (meta.regularMarketChangePercent() != null) return meta.regularMarketChangePercent();

    return previousClose == 0 ? 0 : (price - previousClose) / previousClose * 100;
  }

  private <T> T valueAt(List<T> values, int index) {
    return values == null || index >= values.size() ? null : values.get(index);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChartResponse(ChartData chart) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChartData(List<ChartResult> result) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChartResult(ChartMeta meta, List<Long> timestamp, ChartIndicators indicators) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChartMeta(
    Double regularMarketPrice,
    Double regularMarketChangePercent,
    Double chartPreviousClose,
    Double previousClose,
    Long regularMarketVolume,
    Double fiftyTwoWeekHigh,
    Double fiftyTwoWeekLow,
    String exchangeTimezoneName) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChartIndicators(List<ChartQuote> quote) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ChartQuote(
    List<Double> open,
    List<Double> high,
    List<Double> low,
    List<Double> close,
    List<Long> volume) {
  }
}
