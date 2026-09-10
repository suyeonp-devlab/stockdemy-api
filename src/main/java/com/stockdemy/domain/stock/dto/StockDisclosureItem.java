package com.stockdemy.domain.stock.dto;

import com.stockdemy.infra.dart.dto.DisclosureItem;
import lombok.Builder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Builder
public record StockDisclosureItem(
  String receiptNo,
  String corpName,
  String reportName,
  String receivedAt,
  String sourceUrl
) {

  private static final DateTimeFormatter DART_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("MM.dd");

  // 종목명이 있으면 DART 정식 법인명(에스케이바이오팜) 대신 종목 목록과 같은 이름(SK바이오팜)으로 표시
  public static StockDisclosureItem from(DisclosureItem item, String stockName) {
    return StockDisclosureItem.builder()
      .receiptNo(item.receiptNo())
      .corpName(stockName != null ? stockName : item.corpName())
      .reportName(normalizeSpaces(item.reportName()))
      .receivedAt(toDisplayDate(item.receivedAt()))
      .sourceUrl(item.sourceUrl())
      .build();
  }

  // DART 보고서명의 앞뒤·연속 공백 정리
  private static String normalizeSpaces(String value) {
    return value == null ? null : value.trim().replaceAll("\\s+", " ");
  }

  // DART 접수일자(yyyyMMdd) → MM.dd (DART는 접수 시각을 주지 않고, 앱은 받은 문자열을 그대로 출력함)
  private static String toDisplayDate(String receivedAt) {

    try {
      return LocalDate.parse(receivedAt, DART_DATE_FORMAT).format(DISPLAY_DATE_FORMAT);
    } catch (Exception e) {
      return receivedAt;
    }
  }
}
