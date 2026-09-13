package com.stockdemy.infra.ai;

import com.stockdemy.infra.ai.dto.JournalReviewRequest;
import com.stockdemy.infra.ai.dto.NewsAnalysisRequest;
import com.stockdemy.infra.ai.dto.NewsAnalysisResponse;
import com.stockdemy.infra.ai.dto.StockAnalysisRequest;
import com.stockdemy.infra.ai.dto.StockAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * AI 호출 한도 관리
 *
 * <p>모든 AI 호출이 이 데코레이터를 지나므로 뉴스 분석·종목 분석·일지 복기가 하루 한도를 공유한다.
 * 한도에 닿으면 외부 호출 없이 {@link AiQuotaExceededException}을 던져 배치가 즉시 멈추게 한다.
 */
@Slf4j
@RequiredArgsConstructor
public class QuotaAwareAiClient implements AiClient {

  private final AiClient delegate;
  private final AiQuotaStore quotaStore;
  private final int dailyLimit;

  @Override
  public Optional<NewsAnalysisResponse> analyzeNews(NewsAnalysisRequest request) {
    return call(() -> delegate.analyzeNews(request));
  }

  @Override
  public Optional<StockAnalysisResponse> analyzeStock(StockAnalysisRequest request) {
    return call(() -> delegate.analyzeStock(request));
  }

  @Override
  public Optional<String> reviewJournal(JournalReviewRequest request) {
    return call(() -> delegate.reviewJournal(request));
  }

  private <T> Optional<T> call(Supplier<Optional<T>> supplier) {

    long used = quotaStore.count();

    if (used >= dailyLimit) {
      throw new AiQuotaExceededException("일일 AI 호출 한도 도달 (%d/%d)".formatted(used, dailyLimit));
    }

    // 실패한 호출도 한도를 소모하므로 결과와 무관하게 기록한다
    quotaStore.increase();

    return supplier.get();
  }
}
