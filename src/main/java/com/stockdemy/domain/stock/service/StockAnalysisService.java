package com.stockdemy.domain.stock.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.news.repository.NewsRepository;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.infra.ai.AiClient;
import com.stockdemy.infra.ai.AiQuotaExceededException;
import com.stockdemy.infra.ai.dto.StockAnalysisRequest;
import com.stockdemy.infra.ai.dto.StockAnalysisResponse;
import com.stockdemy.infra.ai.dto.StockNewsDigest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 종목 AI 분석
 *
 * <p>종목 시세와 최근 뉴스 AI 요약으로 AI 신호와 코멘트를 만든다. 하루 AI 호출 한도가 작아
 * 미분석 종목 → 분석이 오래된 종목 순으로 조금씩 채운다. 뉴스 원문은 다시 열지 않고 저장된 요약만
 * 넘긴다. 외부 호출이 길어 트랜잭션을 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockAnalysisService {

  private static final String MARKET_GROUP = "STOCK_MARKET";
  private static final String SECTOR_GROUP = "STOCK_SECTOR";
  private static final String SENTIMENT_GROUP = "AI_SENTIMENT";

  private static final Set<String> SENTIMENTS = Set.of("POSITIVE", "NEUTRAL", "NEGATIVE");

  // 분석 입력으로 쓸 뉴스 범위
  private static final int NEWS_DAYS = 7;
  private static final int NEWS_LIMIT = 3;
  private static final DateTimeFormatter NEWS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  // 미분석 종목 → 분석이 오래된 종목, 같으면 거래대금이 큰 종목
  private static final Comparator<Stock> PRIORITY = Comparator
    .comparing(Stock::getAiAnalyzedAt, Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()))
    .thenComparing(Comparator.<Stock>comparingDouble(StockAnalysisService::tradingValue).reversed());

  private final StockRepository stockRepository;
  private final NewsRepository newsRepository;
  private final AiClient aiClient;
  private final CodeService codeService;

  @Value("${ai.call-interval-ms}")
  private long callIntervalMs;

  // 오늘 분석 이력 여부
  public boolean analyzedToday() {
    return stockRepository.existsByAiAnalyzedAtAfter(LocalDate.now().atStartOfDay());
  }

  // 우선순위 종목부터 한도까지 분석 (분석 건수 반환)
  public int analyzeAll() {

    List<Stock> targets = stockRepository.findAll().stream()
      // 시세를 한 번도 받지 못한 종목은 판단 근거가 없어 제외
      .filter(stock -> stock.getLastPrice() != null)
      .sorted(PRIORITY)
      .toList();

    Map<String, String> marketNames = codeService.getCodeNameMap(MARKET_GROUP);
    Map<String, String> sectorNames = codeService.getCodeNameMap(SECTOR_GROUP);
    Map<String, String> sentimentNames = codeService.getCodeNameMap(SENTIMENT_GROUP);

    int analyzed = 0;

    for (Stock stock : targets) {

      try {
        if (analyze(stock, marketNames, sectorNames, sentimentNames)) {
          analyzed++;
        }

        Thread.sleep(callIntervalMs);

      } catch (AiQuotaExceededException e) {
        log.info("종목 분석 중단: {} ({}건 분석)", e.getMessage(), analyzed);
        return analyzed;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("종목 분석 중단: {}건 분석", analyzed);
        return analyzed;

      } catch (Exception e) {
        log.warn("종목 분석 실패 (stockCode={}): {}", stock.getStockCode(), e.getMessage());
      }
    }

    return analyzed;
  }

  // 종목 1건 분석 후 반영 (실패 시 분석 시각을 갱신하지 않아 다음 배치에서 우선 대상이 된다)
  private boolean analyze(
    Stock stock,
    Map<String, String> marketNames,
    Map<String, String> sectorNames,
    Map<String, String> sentimentNames
  ) {

    List<StockNewsDigest> recentNews = newsRepository.findRecentByStock(
        stock.getStockCode(),
        LocalDateTime.now().minusDays(NEWS_DAYS),
        PageRequest.of(0, NEWS_LIMIT)
      ).stream()
      .map(news -> StockNewsDigest.builder()
        .publishedDate(news.getPublishedAt().format(NEWS_DATE_FORMAT))
        .title(news.getTitle())
        .sentimentName(sentimentNames.get(news.getSentiment()))
        .summary(news.getSummary())
        .build())
      .toList();

    Optional<StockAnalysisResponse> analyzed = aiClient.analyzeStock(StockAnalysisRequest.builder()
      .stockCode(stock.getStockCode())
      .stockName(stock.getStockName())
      .marketName(marketNames.get(stock.getMarket()))
      .sectorName(sectorNames.get(stock.getSector()))
      .lastPrice(stock.getLastPrice())
      .lastChangePercent(stock.getLastChangePercent())
      .week52High(stock.getWeek52High())
      .week52Low(stock.getWeek52Low())
      .per(perShareRatio(stock.getLastPrice(), stock.getEps()))
      .sectorPer(stock.getSectorPer())
      .pbr(perShareRatio(stock.getLastPrice(), stock.getBps()))
      .marketCap(stock.getLastMarketCap())
      .recentNews(recentNews)
      .build());

    if (analyzed.isEmpty()
      || !SENTIMENTS.contains(analyzed.get().sentiment())
      || !StringUtils.hasText(analyzed.get().aiComment())) {
      log.warn("종목 AI 분석 실패로 반영하지 않음 (stockCode={})", stock.getStockCode());
      return false;
    }

    stockRepository.updateAnalysis(
      stock.getStockCode(),
      analyzed.get().sentiment(),
      analyzed.get().aiComment().trim(),
      LocalDateTime.now()
    );

    return true;
  }

  // 주가 배수 (주당 지표가 없거나 0 이하이면 계산하지 않음)
  private Double perShareRatio(Double price, Double perShare) {
    return price == null || perShare == null || perShare <= 0 ? null : price / perShare;
  }

  private static double tradingValue(Stock stock) {

    if (stock.getLastPrice() == null || stock.getLastVolume() == null) return 0;

    return stock.getLastPrice() * stock.getLastVolume();
  }
}
