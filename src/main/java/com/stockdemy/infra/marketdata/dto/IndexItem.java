package com.stockdemy.infra.marketdata.dto;

import lombok.Builder;

@Builder
public record IndexItem(
  String marketCode,
  String marketName,
  double indexValue,
  double changePercent) {
}
