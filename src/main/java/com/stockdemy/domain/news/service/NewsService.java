package com.stockdemy.domain.news.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.news.dto.MentionRankingItem;
import com.stockdemy.domain.news.dto.NewsDetailResponse;
import com.stockdemy.domain.news.dto.NewsItem;
import com.stockdemy.domain.news.dto.NewsListResponse;
import com.stockdemy.domain.news.dto.NewsSearchRequest;
import com.stockdemy.domain.news.dto.RelatedStockItem;
import com.stockdemy.domain.news.dto.SentimentSummaryResponse;
import com.stockdemy.domain.news.entity.News;
import com.stockdemy.domain.news.entity.NewsRelatedStock;
import com.stockdemy.domain.news.repository.NewsQueryRepository;
import com.stockdemy.domain.news.repository.NewsRelatedStockRepository;
import com.stockdemy.domain.news.repository.NewsRepository;
import com.stockdemy.domain.news.repository.NewsRepository.SentimentCount;
import com.stockdemy.domain.news.repository.NewsRepository.StockCount;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.stockdemy.global.util.DateTimeUtil.toCompactDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

  private static final String CATEGORY_GROUP = "NEWS_CATEGORY";
  private static final String SENTIMENT_GROUP = "AI_SENTIMENT";
  private static final String IMPACT_GROUP = "NEWS_IMPACT";

  // 하루 AI 분석 한도가 작아 짧은 기간은 표본이 부족하다
  private static final int SUMMARY_DAYS = 7;

  // 많이 언급된 종목 표시 개수 (모바일 2×2)
  private static final int MENTION_LIMIT = 4;

  private final NewsRepository newsRepository;
  private final NewsQueryRepository newsQueryRepository;
  private final NewsRelatedStockRepository newsRelatedStockRepository;
  private final StockRepository stockRepository;
  private final CodeService codeService;

  // 뉴스 목록 조회
  public NewsListResponse getNewsList(NewsSearchRequest request, Long userId) {

    if (request.favorite() && userId == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    Long favoriteUserId = request.favorite() ? userId : null;
    long totalCount = newsQueryRepository.count(request.category(), request.keyword(), favoriteUserId);
    int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / request.pageSize()));

    List<News> newsList = totalCount == 0 ? List.of() : newsQueryRepository.search(
      request.category(),
      request.keyword(),
      favoriteUserId,
      (request.page() - 1) * request.pageSize(),
      request.pageSize()
    );

    Map<String, String> stockNames = findStockNames(newsList.stream().map(News::getStockCode).toList());
    Map<String, String> categoryNames = codeService.getCodeNameMap(CATEGORY_GROUP);
    Map<String, String> sentimentNames = codeService.getCodeNameMap(SENTIMENT_GROUP);

    List<NewsItem> items = newsList.stream()
      .map(news -> NewsItem.builder()
        .id(news.getNewsId())
        .stockCode(news.getStockCode())
        .stockName(stockNames.get(news.getStockCode()))
        .title(news.getTitle())
        .summary(news.getSummary())
        .publishedAt(toCompactDateTime(news.getPublishedAt()))
        .category(news.getCategory())
        .categoryNm(categoryNames.get(news.getCategory()))
        .sentiment(news.getSentiment())
        .sentimentNm(sentimentNames.get(news.getSentiment()))
        .sourceUrl(news.getSourceUrl())
        .sourceName(news.getSourceName())
        .build())
      .toList();

    return new NewsListResponse(totalCount, totalPages, items);
  }

  // 뉴스 상세 조회
  public NewsDetailResponse getNewsDetail(Long newsId) {

    News news = newsRepository.findById(newsId)
      .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 뉴스입니다."));

    List<NewsRelatedStock> relatedStocks = newsRelatedStockRepository.findByNewsId(newsId);

    Set<String> codes = new HashSet<>();
    codes.add(news.getStockCode());
    relatedStocks.forEach(related -> codes.add(related.getStockCode()));

    Map<String, String> stockNames = findStockNames(codes);
    Map<String, String> impactNames = codeService.getCodeNameMap(IMPACT_GROUP);

    List<RelatedStockItem> relatedItems = relatedStocks.stream()
      .map(related -> RelatedStockItem.builder()
        .stockCode(related.getStockCode())
        .stockName(stockNames.get(related.getStockCode()))
        .impact(related.getImpact())
        .impactNm(impactNames.get(related.getImpact()))
        .build())
      .toList();

    return NewsDetailResponse.builder()
      .id(news.getNewsId())
      .stockCode(news.getStockCode())
      .stockName(stockNames.get(news.getStockCode()))
      .title(news.getTitle())
      .summary(news.getSummary())
      .publishedAt(toCompactDateTime(news.getPublishedAt()))
      .category(news.getCategory())
      .categoryNm(codeService.getCodeName(CATEGORY_GROUP, news.getCategory()))
      .sentiment(news.getSentiment())
      .sentimentNm(codeService.getCodeName(SENTIMENT_GROUP, news.getSentiment()))
      .sourceUrl(news.getSourceUrl())
      .sourceName(news.getSourceName())
      .confidence(news.getConfidence())
      .reasoning(news.getReasoning())
      .relatedStocks(relatedItems)
      .build();
  }

  // 시장 평가 (최근 발행 뉴스의 감성 비율)
  public SentimentSummaryResponse getSentimentSummary() {

    Map<String, Long> counts = newsRepository.countBySentimentSince(summaryFrom()).stream()
      .filter(count -> count.getSentiment() != null)
      .collect(Collectors.toMap(SentimentCount::getSentiment, SentimentCount::getCount));

    long positive = counts.getOrDefault("POSITIVE", 0L);
    long neutral = counts.getOrDefault("NEUTRAL", 0L);
    long negative = counts.getOrDefault("NEGATIVE", 0L);
    int[] percents = toPercents(positive, neutral, negative);

    return new SentimentSummaryResponse(percents[0], percents[1], percents[2], positive + neutral + negative);
  }

  // 많이 언급된 종목 (주체 종목 + 관련 종목 언급 합산)
  public List<MentionRankingItem> getTopMentions() {

    LocalDateTime from = summaryFrom();
    Map<String, Long> mentions = new HashMap<>();

    for (StockCount count : newsRepository.countByStockSince(from)) {
      mentions.merge(count.getStockCode(), count.getCount(), Long::sum);
    }

    for (StockCount count : newsRepository.countRelatedByStockSince(from)) {
      mentions.merge(count.getStockCode(), count.getCount(), Long::sum);
    }

    List<Entry<String, Long>> ranked = mentions.entrySet().stream()
      .sorted(Entry.<String, Long>comparingByValue().reversed().thenComparing(Entry.comparingByKey()))
      .limit(MENTION_LIMIT)
      .toList();

    Map<String, String> stockNames = findStockNames(ranked.stream().map(Entry::getKey).toList());

    return ranked.stream()
      .map(entry -> new MentionRankingItem(entry.getKey(), stockNames.get(entry.getKey()), entry.getValue()))
      .toList();
  }

  private LocalDateTime summaryFrom() {
    return LocalDateTime.now().minusDays(SUMMARY_DAYS);
  }

  private Map<String, String> findStockNames(Collection<String> stockCodes) {

    if (stockCodes.isEmpty()) {
      return Map.of();
    }

    return stockRepository.findAllById(new HashSet<>(stockCodes)).stream()
      .collect(Collectors.toMap(Stock::getStockCode, Stock::getStockName));
  }

  // 건수 → 퍼센트 (반올림 오차로 합이 100을 벗어나지 않도록 나머지가 큰 항목부터 1씩 배분)
  private int[] toPercents(long... counts) {

    int[] percents = new int[counts.length];
    long total = 0;

    for (long count : counts) {
      total += count;
    }

    if (total == 0) {
      return percents;
    }

    long[] remainders = new long[counts.length];
    int assigned = 0;

    for (int i = 0; i < counts.length; i++) {
      percents[i] = (int) (counts[i] * 100 / total);
      remainders[i] = counts[i] * 100 % total;
      assigned += percents[i];
    }

    for (int left = 100 - assigned; left > 0; left--) {

      int largest = 0;

      for (int i = 1; i < counts.length; i++) {
        if (remainders[i] > remainders[largest]) largest = i;
      }

      percents[largest]++;
      remainders[largest] = -1;
    }

    return percents;
  }
}
