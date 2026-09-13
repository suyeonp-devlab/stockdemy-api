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

@Entity
@Table(name = "news_related_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsRelatedStock extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long relatedId;

  // 뉴스 ID
  @Column(nullable = false)
  private Long newsId;

  // 관련 종목 코드
  @Column(nullable = false, length = 20)
  private String stockCode;

  // 영향
  @Column(length = 20)
  private String impact;

  @Builder
  private NewsRelatedStock(Long newsId, String stockCode, String impact) {
    this.newsId = newsId;
    this.stockCode = stockCode;
    this.impact = impact;
  }
}
