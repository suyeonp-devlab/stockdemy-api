package com.stockdemy.infra.ai;

/**
 * AI 일일 호출 한도 초과
 *
 * <p>무료 티어는 모델당 하루 호출 수가 매우 작다. 한도에 닿으면 남은 대상을 계속 시도해도 전부
 * 실패하므로, 호출 측이 배치를 즉시 멈출 수 있도록 일반 실패와 구분해 던진다.
 */
public class AiQuotaExceededException extends RuntimeException {

  public AiQuotaExceededException(String message) {
    super(message);
  }
}
