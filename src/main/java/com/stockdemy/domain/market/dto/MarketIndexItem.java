package com.stockdemy.domain.market.dto;

import com.stockdemy.infra.marketdata.dto.IndexItem;
import lombok.Builder;

import static com.stockdemy.global.util.NumberUtil.round2;

@Builder
public record MarketIndexItem(
  String marketCode,
  String marketName,
  Double indexValue,
  Double changePercent
) {

  public static MarketIndexItem from(IndexItem item) {
    return MarketIndexItem.builder()
      .marketCode(item.marketCode())
      .marketName(item.marketName())
      .indexValue(round2(item.indexValue()))
      .changePercent(round2(item.changePercent()))
      .build();
  }
}
