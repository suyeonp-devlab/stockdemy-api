package com.stockdemy.domain.stock.repository;

import com.stockdemy.domain.stock.entity.UserFavoriteStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserFavoriteStockRepository extends JpaRepository<UserFavoriteStock, Long> {

  // 사용자의 관심종목 코드
  @Query("SELECT f.stockCode FROM UserFavoriteStock f WHERE f.userId = :userId")
  List<String> findStockCodesByUserId(@Param("userId") Long userId);

  boolean existsByUserIdAndStockCode(Long userId, String stockCode);

  // 관심종목 등록 (이미 있으면 무시)
  @Modifying
  @Query(value = """
    INSERT INTO user_favorite_stocks (user_id, stock_code, created_at)
    VALUES (:userId, :stockCode, :createdAt)
    ON CONFLICT (user_id, stock_code) DO NOTHING
    """, nativeQuery = true)
  void insertIfAbsent(
    @Param("userId") Long userId,
    @Param("stockCode") String stockCode,
    @Param("createdAt") LocalDateTime createdAt
  );

  // 관심종목 해제
  @Modifying
  @Query("DELETE FROM UserFavoriteStock f WHERE f.userId = :userId AND f.stockCode = :stockCode")
  void deleteByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);
}
