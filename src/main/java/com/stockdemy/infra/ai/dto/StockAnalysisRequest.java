package com.stockdemy.infra.ai.dto;

import lombok.Builder;

import java.util.List;

/** 종목 분석 입력 (수집하지 못한 값은 null → 프롬프트에서 "정보 없음") */
@Builder
public record StockAnalysisRequest(
  String stockCode,
  String stockName,
  String marketName,
  String sectorName,
  Double lastPrice,
  Double lastChangePercent,
  Double week52High,
  Double week52Low,
  Double per,
  Double sectorPer,
  Double pbr,
  Long marketCap,
  List<StockNewsDigest> recentNews) {
}
