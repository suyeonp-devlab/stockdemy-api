package com.stockdemy.domain.market.dto;

import lombok.Builder;

@Builder
public record SectorSummaryItem(
  String sector,
  String sectorNm,
  Double changePercent
) {
}
