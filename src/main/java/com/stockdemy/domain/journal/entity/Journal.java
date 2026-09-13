package com.stockdemy.domain.journal.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "journals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Journal extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long journalId;

  // 작성자 ID
  @Column(nullable = false)
  private Long userId;

  // 종목 코드
  @Column(nullable = false, length = 20)
  private String stockCode;

  // AI 복기 상태
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private JournalStatus status;

  // 시장 구분 (작성 시점 종목 마스터 기준)
  @Column(nullable = false, length = 20)
  private String market;

  // 업종 (작성 시점 종목 마스터 기준)
  @Column(nullable = false, length = 20)
  private String sector;

  // 매매 구분
  @Column(nullable = false, length = 20)
  private String tradeType;

  // 매매일
  @Column(nullable = false)
  private LocalDate tradeDate;

  // 매매 시각
  private LocalTime tradeTime;

  // 매매 단가
  @Column(nullable = false)
  private Double price;

  // 수량
  @Column(nullable = false)
  private Integer quantity;

  // 메모
  private String memo;

  // AI 복기 코멘트
  private String aiComment;

  @Builder
  private Journal(
    Long userId,
    String stockCode,
    String market,
    String sector,
    String tradeType,
    LocalDate tradeDate,
    LocalTime tradeTime,
    Double price,
    Integer quantity,
    String memo
  ) {
    this.userId = userId;
    this.stockCode = stockCode;
    this.status = JournalStatus.STANDBY;
    this.market = market;
    this.sector = sector;
    this.tradeType = tradeType;
    this.tradeDate = tradeDate;
    this.tradeTime = tradeTime;
    this.price = price;
    this.quantity = quantity;
    this.memo = memo;
  }

  // 매매 기록 수정
  public void update(
    String stockCode,
    String market,
    String sector,
    String tradeType,
    LocalDate tradeDate,
    LocalTime tradeTime,
    Double price,
    Integer quantity,
    String memo
  ) {
    this.stockCode = stockCode;
    this.market = market;
    this.sector = sector;
    this.tradeType = tradeType;
    this.tradeDate = tradeDate;
    this.tradeTime = tradeTime;
    this.price = price;
    this.quantity = quantity;
    this.memo = memo;
  }

  // 분석 대기 여부 (수정·복기 요청 가능 상태)
  public boolean isStandby() {
    return status == JournalStatus.STANDBY;
  }

  // AI 복기 요청 접수 (분석 대기 → 분석 중)
  public void requestReview() {
    this.status = JournalStatus.PENDING;
  }
}
