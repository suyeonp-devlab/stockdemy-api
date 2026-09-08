package com.stockdemy.domain.stock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "price_bars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceBar {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long barId;

  // 종목 코드
  @Column(nullable = false, length = 20)
  private String stockCode;

  // 봉 기준일
  @Column(nullable = false)
  private LocalDate barDate;

  // 시가
  @Column(nullable = false)
  private Double open;

  // 고가
  @Column(nullable = false)
  private Double high;

  // 저가
  @Column(nullable = false)
  private Double low;

  // 종가
  @Column(nullable = false)
  private Double close;

  // 거래량
  @Column(nullable = false)
  private Long volume;

  // 거래대금
  @Column(nullable = false)
  private Long tradingValue;

  @Builder
  private PriceBar(
    String stockCode,
    LocalDate barDate,
    Double open,
    Double high,
    Double low,
    Double close,
    Long volume,
    Long tradingValue
  ) {
    this.stockCode = stockCode;
    this.barDate = barDate;
    this.open = open;
    this.high = high;
    this.low = low;
    this.close = close;
    this.volume = volume;
    this.tradingValue = tradingValue;
  }
}
