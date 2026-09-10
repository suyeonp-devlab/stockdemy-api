package com.stockdemy.domain.stock.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * 시장별 정규장 운영시간과 종가 확정 시각
 *
 * <p>장중 여부의 단일 기준이다. Yahoo 응답의 거래시간 정보는 KRX 마감을 15:00으로 내려주는 등
 * 실제와 달라 사용하지 않는다. 시장 현지 시간대로 계산하므로 미국 서머타임은 자동 반영되며,
 * 공휴일은 판별하지 않는다.
 *
 * <p>{@code settleTime}은 Yahoo에 당일 종가가 확정 반영되는 시각이다. KRX는 지연 시세라 장 종료보다
 * 늦다. {@code application.yml}의 장마감 배치 cron({@code market-data.close.*})과 맞춰야 한다.
 */
@Getter
@RequiredArgsConstructor
public enum MarketSession {

  KOSPI("KOSPI", ZoneId.of("Asia/Seoul"), LocalTime.of(9, 0), LocalTime.of(15, 30), LocalTime.of(18, 10)),
  KOSDAQ("KOSDAQ", ZoneId.of("Asia/Seoul"), LocalTime.of(9, 0), LocalTime.of(15, 30), LocalTime.of(18, 10)),
  NASDAQ("NASDAQ", ZoneId.of("America/New_York"), LocalTime.of(9, 30), LocalTime.of(16, 0), LocalTime.of(16, 30));

  private final String market;
  private final ZoneId zone;
  private final LocalTime open;
  private final LocalTime close;
  private final LocalTime settleTime;

  // 정규장 운영 여부
  public boolean isOpen(Instant at) {

    ZonedDateTime local = at.atZone(zone);
    DayOfWeek day = local.getDayOfWeek();

    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;

    LocalTime time = local.toLocalTime();

    return !time.isBefore(open) && time.isBefore(close);
  }

  // 일봉 종가 확정 여부 (지난 날짜는 확정, 당일은 확정 시각 이후)
  public boolean isSettled(LocalDate barDate, Instant at) {

    ZonedDateTime local = at.atZone(zone);
    LocalDate today = local.toLocalDate();

    if (barDate.isBefore(today)) return true;
    if (barDate.isAfter(today)) return false;

    return !local.toLocalTime().isBefore(settleTime);
  }

  // 시장 코드로 조회
  public static Optional<MarketSession> of(String market) {
    return Arrays.stream(values()).filter(session -> session.market.equals(market)).findFirst();
  }
}
