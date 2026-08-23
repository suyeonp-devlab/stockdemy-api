package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record StockAnalysisRequest(
  String stockCode,
  String marketName,
  String sectorName,
  double prevClose,
  double week52High,
  double week52Low,
  int sharesOutstanding,
  double eps,
  double bps,
  double sectorPer,
  double lastPrice,
  double lastChangePercent,
  int lastVolume,
  List<String> newsSourceUrls) {
}
