package com.stockdemy.domain.stock.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseTimeEntity {

  @Id
  @Column(length = 20)
  private String stockCode;

  // 종목명
  @Column(nullable = false)
  private String stockName;

  // 시장 구분
  @Column(nullable = false, length = 20)
  private String market;

  // 업종
  @Column(nullable = false, length = 20)
  private String sector;

  // AI 신호
  @Column(length = 20)
  private String sentiment;

  // 뉴스 검색어 (비어 있으면 종목명 사용)
  @Column(length = 100)
  private String searchKeyword;

  // 전일 종가
  private Double prevClose;

  // 52주 최고가
  @Column(name = "week52_high")
  private Double week52High;

  // 52주 최저가
  @Column(name = "week52_low")
  private Double week52Low;

  // 발행 주식 수
  private Long sharesOutstanding;

  // 외국인 지분율
  private Double foreignOwnership;

  // 주당순이익
  private Double eps;

  // 주당순자산가치
  private Double bps;

  // 연간 배당금
  private Double annualDividend;

  // 업종 평균 PER
  private Double sectorPer;

  // AI 코멘트
  private String aiComment;

  // 최근 동기화 현재가
  private Double lastPrice;

  // 최근 동기화 등락률
  private Double lastChangePercent;

  // 최근 동기화 시가총액
  private Long lastMarketCap;

  // 최근 동기화 거래량
  private Long lastVolume;

  // 최근 동기화 시각
  private LocalDateTime lastSyncedAt;

  // 펀더멘털 최근 수집 시각
  private LocalDateTime fundSyncedAt;

  // 뉴스 검색에 사용할 키워드
  public String newsKeyword() {
    return searchKeyword == null || searchKeyword.isBlank() ? stockName : searchKeyword;
  }

  // 시세 동기화 결과 반영
  public void applyQuote(
    double price,
    double previousClose,
    double changePercent,
    long volume,
    double week52High,
    double week52Low
  ) {
    this.lastPrice = price;
    this.prevClose = previousClose;
    this.lastChangePercent = changePercent;
    this.lastVolume = volume;

    // 프로바이더가 값을 못 준 경우(0) 기존 값을 유지한다
    if (week52High > 0) this.week52High = week52High;
    if (week52Low > 0) this.week52Low = week52Low;

    this.lastSyncedAt = LocalDateTime.now();
  }
}
