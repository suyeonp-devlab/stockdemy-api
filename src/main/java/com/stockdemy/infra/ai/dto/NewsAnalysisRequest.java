package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record NewsAnalysisRequest(
  String stockCode,
  String title,
  String sourceUrl,
  List<String> candidateStockCode) {
}
