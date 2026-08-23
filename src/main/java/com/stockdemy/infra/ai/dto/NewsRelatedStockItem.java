package com.stockdemy.infra.ai.dto;

import lombok.Builder;

@Builder
public record NewsRelatedStockItem(String stockCode, String impact) {
}
