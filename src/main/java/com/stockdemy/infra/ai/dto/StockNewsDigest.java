package com.stockdemy.infra.ai.dto;

import lombok.Builder;

/** 종목 분석 입력용 뉴스 요약 (원문을 다시 열지 않도록 저장된 AI 요약만 전달) */
@Builder
public record StockNewsDigest(
  String publishedDate,
  String title,
  String sentimentName,
  String summary) {
}
