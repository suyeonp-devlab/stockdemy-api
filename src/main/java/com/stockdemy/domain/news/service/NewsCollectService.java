package com.stockdemy.domain.news.service;

import com.stockdemy.domain.news.entity.News;
import com.stockdemy.domain.news.repository.NewsRepository;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.news.repository.NewsRepository.StockNewsTime;
import com.stockdemy.infra.ai.AiClient;
import com.stockdemy.infra.ai.AiQuotaExceededException;
import com.stockdemy.infra.ai.dto.NewsAnalysisRequest;
import com.stockdemy.infra.ai.dto.NewsAnalysisResponse;
import com.stockdemy.infra.ai.dto.NewsCandidateStock;
import com.stockdemy.infra.ai.dto.NewsRelatedStockItem;
import com.stockdemy.infra.news.NewsProvider;
import com.stockdemy.infra.news.dto.NewsArticleItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 뉴스 수집
 *
 * <p>언론사 RSS에서 최근 기사를 모아 제목에 종목 검색어가 있는 기사만 AI로 분석하고 저장한다.
 * 같은 기사를 여러 종목에 중복 저장하지 않는다. AI 분석에 실패한 기사는 저장하지 않고 다음 수집에서
 * 다시 후보가 된다. 외부 호출이 길어 트랜잭션을 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectService {

  private static final String MARKET_NASDAQ = "NASDAQ";
  private static final String CATEGORY_DOMESTIC = "DOMESTIC";
  private static final String CATEGORY_OVERSEAS = "OVERSEAS";

  private static final Set<String> SENTIMENTS = Set.of("POSITIVE", "NEUTRAL", "NEGATIVE");
  private static final Set<String> IMPACTS = Set.of("BENEFIT", "LIMITED", "ADVERSE");

  // 종목명 뒤에 바로 붙는 조사 (한글은 "삼성전자의"처럼 띄어쓰기 없이 이어진다)
  private static final Set<Character> PARTICLES =
    Set.of('은', '는', '이', '가', '을', '를', '의', '에', '와', '과', '도', '로', '으', '만', '부', '까', '랑', '나');

  private final NewsProvider newsProvider;
  private final AiClient aiClient;
  private final NewsRepository newsRepository;
  private final NewsSaveService newsSaveService;
  private final StockRepository stockRepository;

  @Value("${news.per-stock-limit}")
  private int perStockLimit;

  @Value("${news.recent-hours}")
  private int recentHours;

  @Value("${news.max-related-stocks}")
  private int maxRelatedStocks;

  @Value("${news.ai-interval-ms}")
  private long aiIntervalMs;

  // 전체 추적 종목 뉴스 수집 (저장 건수 반환)
  public int collectAll() {

    List<Stock> stocks = stockRepository.findAll();

    if (stocks.isEmpty()) {
      return 0;
    }

    // 최신 기사부터 고르도록 정렬
    List<NewsArticleItem> candidates = newsProvider.fetchRecent().stream()
      .filter(this::isCandidate)
      .sorted(Comparator.comparing(NewsArticleItem::publishedAt).reversed())
      .toList();

    log.info("뉴스 수집 후보: {}건", candidates.size());

    if (candidates.isEmpty()) {
      return 0;
    }

    // 관련 종목 판단 후보로 쓸 종목명 맵
    Map<String, String> stockNames = new LinkedHashMap<>();
    stocks.forEach(stock -> stockNames.put(stock.getStockCode(), stock.getStockName()));

    // 한 기사는 한 종목에만 저장한다 (여러 종목명이 제목에 함께 나오는 경우)
    Set<String> usedUrls = new HashSet<>();
    int saved = 0;

    for (Stock stock : prioritize(stocks)) {

      Pattern keyword = Pattern.compile(Pattern.quote(stock.newsKeyword()), Pattern.CASE_INSENSITIVE);

      List<NewsArticleItem> matched = candidates.stream()
        .filter(article -> !usedUrls.contains(article.sourceUrl()))
        .filter(article -> matchesKeyword(article.title(), keyword))
        .limit(perStockLimit)
        .toList();

      for (NewsArticleItem article : matched) {

        usedUrls.add(article.sourceUrl());

        try {
          if (analyzeAndSave(stock, article, stockNames)) {
            saved++;
          }

          Thread.sleep(aiIntervalMs);

        } catch (AiQuotaExceededException e) {
          log.info("뉴스 수집 중단: {} ({}건 저장)", e.getMessage(), saved);
          return saved;

        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("뉴스 수집 중단: {}건 저장", saved);
          return saved;

        } catch (Exception e) {
          log.warn("뉴스 저장 실패 (stockCode={}, url={}): {}", stock.getStockCode(), article.sourceUrl(), e.getMessage());
        }
      }
    }

    return saved;
  }

  /**
   * 수집 순서 결정
   *
   * <p>하루 AI 호출 한도가 작아 모든 종목을 다룰 수 없다. 뉴스가 오래 없던 종목을 먼저 채워 종목별
   * 편중을 줄이고, 같은 조건이면 거래대금이 큰 종목을 먼저 본다.
   */
  private List<Stock> prioritize(List<Stock> stocks) {

    Map<String, LocalDateTime> lastNewsAt = newsRepository.findLatestPublishedAtByStock().stream()
      .collect(Collectors.toMap(
        StockNewsTime::getStockCode,
        StockNewsTime::getPublishedAt,
        (first, second) -> first));

    Comparator<Stock> byNewsGap = Comparator.comparing(
      stock -> lastNewsAt.getOrDefault(stock.getStockCode(), LocalDateTime.MIN));

    Comparator<Stock> byTradingValue = Comparator.comparingDouble(this::tradingValue).reversed();

    return stocks.stream().sorted(byNewsGap.thenComparing(byTradingValue)).toList();
  }

  private double tradingValue(Stock stock) {

    if (stock.getLastPrice() == null || stock.getLastVolume() == null) return 0;

    return stock.getLastPrice() * stock.getLastVolume();
  }

  // 수집 후보 기사 판별
  private boolean isCandidate(NewsArticleItem article) {

    if (article.sourceUrl() == null || article.sourceUrl().isBlank()) return false;
    if (article.title() == null || article.publishedAt() == null) return false;
    if (article.publishedAt().isBefore(LocalDateTime.now().minusHours(recentHours))) return false;

    return !newsRepository.existsBySourceUrl(article.sourceUrl());
  }

  /**
   * 제목에 종목 검색어가 독립된 단어로 있는지 판별
   *
   * <p>부분일치를 그대로 쓰면 "KT"가 "KTB", "한화"가 "한화오션"에 걸려 다른 회사 기사를 가져온다.
   * 앞은 문자·숫자가 아니어야 하고, 뒤는 조사만 허용한다. 대신 "현대차그룹"처럼 이름이 이어지는
   * 기사는 수집되지 않는다.
   */
  private boolean matchesKeyword(String title, Pattern keyword) {

    Matcher matcher = keyword.matcher(title);

    while (matcher.find()) {
      if (isWordStart(title, matcher.start()) && isWordEnd(title, matcher.end())) {
        return true;
      }
    }

    return false;
  }

  private boolean isWordStart(String title, int start) {
    return start == 0 || !Character.isLetterOrDigit(title.charAt(start - 1));
  }

  private boolean isWordEnd(String title, int end) {

    if (end >= title.length()) return true;

    char next = title.charAt(end);

    return !Character.isLetterOrDigit(next) || PARTICLES.contains(next);
  }

  // AI 분석 후 저장 (분석 실패 시 저장하지 않음)
  private boolean analyzeAndSave(Stock stock, NewsArticleItem article, Map<String, String> stockNames) {

    Optional<NewsAnalysisResponse> analyzed = aiClient.analyzeNews(NewsAnalysisRequest.builder()
      .stockCode(stock.getStockCode())
      .stockName(stock.getStockName())
      .title(article.title())
      .sourceUrl(article.sourceUrl())
      .candidateStocks(toCandidates(stock.getStockCode(), stockNames))
      .build());

    if (analyzed.isEmpty() || !SENTIMENTS.contains(analyzed.get().sentiment())) {
      log.warn("뉴스 AI 분석 실패로 저장하지 않음 (stockCode={}, url={})", stock.getStockCode(), article.sourceUrl());
      return false;
    }

    NewsAnalysisResponse analysis = analyzed.get();

    News news = News.builder()
      .stockCode(stock.getStockCode())
      .title(article.title())
      .summary(analysis.summary())
      .publishedAt(article.publishedAt())
      .category(MARKET_NASDAQ.equals(stock.getMarket()) ? CATEGORY_OVERSEAS : CATEGORY_DOMESTIC)
      .sentiment(analysis.sentiment())
      .sourceUrl(article.sourceUrl())
      .sourceName(article.sourceName())
      .confidence(Math.clamp(analysis.confidence(), 0, 100))
      .reasoning(analysis.reasoning())
      .build();

    newsSaveService.save(news, toRelatedStocks(analysis, stock.getStockCode(), stockNames));

    return true;
  }

  // 관련 종목 후보 (주체 종목 제외)
  private List<NewsCandidateStock> toCandidates(String stockCode, Map<String, String> stockNames) {

    List<NewsCandidateStock> candidates = new ArrayList<>();

    stockNames.forEach((code, name) -> {
      if (!code.equals(stockCode)) {
        candidates.add(NewsCandidateStock.builder().stockCode(code).stockName(name).build());
      }
    });

    return candidates;
  }

  // AI가 지목한 관련 종목 정리 (추적 종목·영향 코드만, 중복 제거)
  private List<NewsSaveService.RelatedStock> toRelatedStocks(
    NewsAnalysisResponse analysis,
    String stockCode,
    Map<String, String> stockNames
  ) {

    if (analysis.relatedStocks() == null) {
      return List.of();
    }

    return analysis.relatedStocks().stream()
      .filter(related -> related.stockCode() != null && !related.stockCode().equals(stockCode))
      .filter(related -> stockNames.containsKey(related.stockCode()))
      .filter(related -> IMPACTS.contains(related.impact()))
      .collect(Collectors.toMap(NewsRelatedStockItem::stockCode, related -> related, (first, second) -> first, LinkedHashMap::new))
      .values().stream()
      .limit(maxRelatedStocks)
      .map(related -> new NewsSaveService.RelatedStock(related.stockCode(), related.impact()))
      .toList();
  }
}
