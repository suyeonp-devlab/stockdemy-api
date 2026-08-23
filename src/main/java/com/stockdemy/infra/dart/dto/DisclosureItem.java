package com.stockdemy.infra.dart.dto;

import lombok.Builder;

@Builder
public record DisclosureItem(
  String receiptNo,
  String corpName,
  String stockCode,
  String reportName,
  String receivedAt,
  String sourceUrl) {
}
