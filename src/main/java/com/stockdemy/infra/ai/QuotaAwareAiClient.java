package com.stockdemy.infra.ai;

import com.stockdemy.infra.ai.dto.JournalReviewRequest;
import com.stockdemy.infra.ai.dto.NewsAnalysisRequest;
import com.stockdemy.infra.ai.dto.NewsAnalysisResponse;
import com.stockdemy.infra.ai.dto.StockAnalysisRequest;
import com.stockdemy.infra.ai.dto.StockAnalysisResponse;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * AI 호출 한도 관리
 *
 * <p>모든 AI 호출이 이 데코레이터를 지나며 용도별 하루 한도를 적용한다. 무료 티어 한도가 프로젝트·모델
 * 단위라 배치가 한도를 다 쓰면 사용자가 요청한 일지 복기가 실패하므로 용도별로 나눈다. 한도에 닿으면
 * 외부 호출 없이 {@link AiQuotaExceededException}을 던져 배치가 즉시 멈추게 한다.
 */
@RequiredArgsConstructor
public class QuotaAwareAiClient implements AiClient {

  private final AiClient delegate;
  private final AiQuotaStore quotaStore;
  private final Map<AiUsage, Integer> dailyLimits;

  @Override
  public Optional<NewsAnalysisResponse> analyzeNews(NewsAnalysisRequest request) {
    return call(AiUsage.NEWS, () -> delegate.analyzeNews(request));
  }

  @Override
  public Optional<StockAnalysisResponse> analyzeStock(StockAnalysisRequest request) {
    return call(AiUsage.STOCK, () -> delegate.analyzeStock(request));
  }

  @Override
  public Optional<String> reviewJournal(JournalReviewRequest request) {
    return call(AiUsage.JOURNAL, () -> delegate.reviewJournal(request));
  }

  private <T> Optional<T> call(AiUsage usage, Supplier<Optional<T>> supplier) {

    int limit = dailyLimits.getOrDefault(usage, 0);
    long used = quotaStore.count(usage);

    if (used >= limit) {
      throw new AiQuotaExceededException("일일 AI 호출 한도 도달 (%s %d/%d)".formatted(usage.getLabel(), used, limit));
    }

    // 실패한 호출도 한도를 소모하므로 결과와 무관하게 기록한다
    quotaStore.increase(usage);

    return supplier.get();
  }
}
