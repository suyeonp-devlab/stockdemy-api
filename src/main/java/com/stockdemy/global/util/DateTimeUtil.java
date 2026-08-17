package com.stockdemy.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 날짜 관련 공통 유틸 */
public final class DateTimeUtil {

  private DateTimeUtil() {
  }

  // LocalDateTime → yyyyMMddHHmmss 변환
  public static String toCompactDateTime(LocalDateTime dateTime) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    return dateTime == null ? null : dateTime.format(formatter);
  }
}
