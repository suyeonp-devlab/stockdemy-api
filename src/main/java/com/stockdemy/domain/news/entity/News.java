package com.stockdemy.domain.news.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class News extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long newsId;

  // 주체 종목 코드
  @Column(nullable = false, length = 20)
  private String stockCode;

  // 제목
  @Column(nullable = false, length = 500)
  private String title;

  // AI 요약
  private String summary;

  // 발행일시
  @Column(nullable = false)
  private LocalDateTime publishedAt;

  // 카테고리
  @Column(length = 20)
  private String category;

  // AI 신호
  @Column(length = 20)
  private String sentiment;

  // 원문 URL
  @Column(nullable = false, length = 500)
  private String sourceUrl;

  // 언론사명
  private String sourceName;

  // AI 신호 퍼센트
  private Integer confidence;

  // AI 분석 근거
  private String reasoning;

  @Builder
  private News(
    String stockCode,
    String title,
    String summary,
    LocalDateTime publishedAt,
    String category,
    String sentiment,
    String sourceUrl,
    String sourceName,
    Integer confidence,
    String reasoning
  ) {
    this.stockCode = stockCode;
    this.title = title;
    this.summary = summary;
    this.publishedAt = publishedAt;
    this.category = category;
    this.sentiment = sentiment;
    this.sourceUrl = sourceUrl;
    this.sourceName = sourceName;
    this.confidence = confidence;
    this.reasoning = reasoning;
  }
}
