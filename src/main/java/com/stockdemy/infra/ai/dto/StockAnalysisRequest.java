package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record StockAnalysisRequest(
  String stockCode,
  String stockName,
  String marketName,
  String sectorName,
  double lastPrice,
  double lastChangePercent,
  double week52High,
  double week52Low,
  double per,
  double sectorPer,
  double pbr,
  long marketCap,
  List<String> newsSourceUrls) {
}
