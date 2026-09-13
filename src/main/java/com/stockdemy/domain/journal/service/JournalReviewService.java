package com.stockdemy.domain.journal.service;

import com.stockdemy.domain.code.service.CodeService;
import com.stockdemy.domain.journal.entity.Journal;
import com.stockdemy.domain.journal.entity.JournalStatus;
import com.stockdemy.domain.journal.repository.JournalRepository;
import com.stockdemy.domain.news.repository.NewsRepository;
import com.stockdemy.domain.stock.entity.Stock;
import com.stockdemy.domain.stock.repository.PriceBarRepository;
import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.store.StockQuoteStore;
import com.stockdemy.infra.ai.AiClient;
import com.stockdemy.infra.ai.AiQuotaExceededException;
import com.stockdemy.infra.ai.dto.JournalReviewRequest;
import com.stockdemy.infra.ai.dto.StockNewsDigest;
import com.stockdemy.infra.ai.dto.TradeDayBar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 일지 AI 복기 처리
 *
 * <p>분석 중(PENDING) 일지를 요청 순서대로 처리한다. 요청 상태가 DB에 남아 있어 재기동해도 이어서 처리된다.
 * 하루 AI 한도에 닿으면 분석 중 상태를 그대로 두고 멈춰, 한도가 풀린 뒤 다음 주기에 처리한다. 한도와
 * 무관한 실패는 분석 대기로 되돌려 사용자가 다시 요청할 수 있게 한다. 외부 호출이 길어 트랜잭션을 두지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JournalReviewService {

  private static final String MARKET_GROUP = "STOCK_MARKET";
  private static final String TRADE_TYPE_GROUP = "TRADE_TYPE";
  private static final String SENTIMENT_GROUP = "AI_SENTIMENT";

  // 복기 입력으로 쓸 거래일 전후 뉴스 범위
  private static final int NEWS_DAYS_AROUND = 3;
  private static final int NEWS_LIMIT = 3;
  private static final DateTimeFormatter NEWS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final JournalRepository journalRepository;
  private final StockRepository stockRepository;
  private final PriceBarRepository priceBarRepository;
  private final NewsRepository newsRepository;
  private final StockQuoteStore stockQuoteStore;
  private final AiClient aiClient;
  private final CodeService codeService;

  @Value("${ai.call-interval-ms}")
  private long callIntervalMs;

  // 복기 대기열 처리 (완료 건수 반환)
  public int reviewPending() {

    List<Journal> pending = journalRepository.findByStatusOrderByUpdatedAtAscJournalIdAsc(JournalStatus.PENDING);

    if (pending.isEmpty()) {
      return 0;
    }

    Map<String, String> marketNames = codeService.getCodeNameMap(MARKET_GROUP);
    Map<String, String> tradeTypeNames = codeService.getCodeNameMap(TRADE_TYPE_GROUP);
    Map<String, String> sentimentNames = codeService.getCodeNameMap(SENTIMENT_GROUP);

    int completed = 0;

    for (Journal journal : pending) {

      try {
        Optional<String> aiComment = aiClient.reviewJournal(toRequest(journal, marketNames, tradeTypeNames, sentimentNames));

        if (aiComment.isPresent() && StringUtils.hasText(aiComment.get())) {
          journalRepository.changeStatus(
            journal.getJournalId(), JournalStatus.PENDING, JournalStatus.DONE, aiComment.get().trim(), LocalDateTime.now());
          completed++;
        } else {
          log.warn("일지 AI 복기 실패로 분석 대기로 되돌림 (journalId={})", journal.getJournalId());
          revertToStandby(journal);
        }

        Thread.sleep(callIntervalMs);

      } catch (AiQuotaExceededException e) {
        log.info("일지 복기 중단: {} ({}건 완료, 남은 요청은 한도가 풀린 뒤 처리)", e.getMessage(), completed);
        return completed;

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("일지 복기 중단: {}건 완료", completed);
        return completed;

      } catch (Exception e) {
        log.warn("일지 AI 복기 실패로 분석 대기로 되돌림 (journalId={}): {}", journal.getJournalId(), e.getMessage());
        revertToStandby(journal);
      }
    }

    return completed;
  }

  // 복기 입력 구성 (매매 기록 + 거래일 일봉 + 이후 흐름 + 거래일 전후 뉴스 요약)
  private JournalReviewRequest toRequest(
    Journal journal,
    Map<String, String> marketNames,
    Map<String, String> tradeTypeNames,
    Map<String, String> sentimentNames
  ) {

    Stock stock = stockRepository.findById(journal.getStockCode()).orElse(null);

    TradeDayBar tradeDayBar = priceBarRepository
      .findFirstByStockCodeAndBarDateLessThanEqualOrderByBarDateDesc(journal.getStockCode(), journal.getTradeDate())
      .map(bar -> new TradeDayBar(bar.getBarDate(), bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose()))
      .orElse(null);

    Double currentPrice = findCurrentPrice(stock);
    Double changeSinceTrade = currentPrice == null ? null : (currentPrice - journal.getPrice()) / journal.getPrice() * 100;

    List<StockNewsDigest> nearbyNews = newsRepository.findByStockBetween(
        journal.getStockCode(),
        journal.getTradeDate().minusDays(NEWS_DAYS_AROUND).atStartOfDay(),
        journal.getTradeDate().plusDays(NEWS_DAYS_AROUND + 1L).atStartOfDay(),
        PageRequest.of(0, NEWS_LIMIT)
      ).stream()
      .map(news -> StockNewsDigest.builder()
        .publishedDate(news.getPublishedAt().format(NEWS_DATE_FORMAT))
        .title(news.getTitle())
        .sentimentName(sentimentNames.get(news.getSentiment()))
        .summary(news.getSummary())
        .build())
      .toList();

    return JournalReviewRequest.builder()
      .stockCode(journal.getStockCode())
      .stockName(stock == null ? journal.getStockCode() : stock.getStockName())
      .marketName(marketNames.get(journal.getMarket()))
      .tradeTypeName(tradeTypeNames.get(journal.getTradeType()))
      .tradeDate(journal.getTradeDate())
      .tradeTime(journal.getTradeTime())
      .price(journal.getPrice())
      .quantity(journal.getQuantity())
      .memo(journal.getMemo())
      .tradeDayBar(tradeDayBar)
      .currentPrice(currentPrice)
      .changeSinceTrade(changeSinceTrade)
      .nearbyNews(nearbyNews)
      .build();
  }

  // 현재가 (Redis 실시간 시세 우선, 없으면 DB의 마지막 장마감 값)
  private Double findCurrentPrice(Stock stock) {

    if (stock == null) return null;

    try {
      StockQuoteStore.Snapshot snapshot = stockQuoteStore.findAll(List.of(stock.getStockCode())).get(stock.getStockCode());

      if (snapshot != null) {
        return snapshot.price();
      }

    } catch (Exception e) {
      log.warn("시세 캐시 조회 실패, DB 값으로 대체합니다: {}", e.getMessage());
    }

    return stock.getLastPrice();
  }

  private void revertToStandby(Journal journal) {
    journalRepository.changeStatus(
      journal.getJournalId(), JournalStatus.PENDING, JournalStatus.STANDBY, null, LocalDateTime.now());
  }
}
