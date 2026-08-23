package com.stockdemy.infra.ai.dto;

import lombok.Builder;

@Builder
public record StockAnalysisResponse(String sentiment, String aiComment) {
}
