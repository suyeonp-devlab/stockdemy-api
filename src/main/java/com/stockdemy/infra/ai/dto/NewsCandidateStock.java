package com.stockdemy.infra.ai.dto;

import lombok.Builder;

@Builder
public record NewsCandidateStock(String stockCode, String stockName) {
}
