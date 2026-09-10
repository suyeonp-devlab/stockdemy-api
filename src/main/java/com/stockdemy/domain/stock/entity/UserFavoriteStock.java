package com.stockdemy.domain.stock.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 관심종목
 *
 * <p>등록은 연속 클릭 같은 동시 요청에도 중복 오류가 나지 않도록 리포지토리의
 * {@code INSERT ... ON CONFLICT DO NOTHING}으로 처리한다.
 */
@Entity
@Table(name = "user_favorite_stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteStock extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long favoriteId;

  // 사용자 ID
  @Column(nullable = false)
  private Long userId;

  // 종목 코드
  @Column(nullable = false, length = 20)
  private String stockCode;
}
