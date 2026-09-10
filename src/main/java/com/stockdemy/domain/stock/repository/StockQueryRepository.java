package com.stockdemy.domain.stock.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockdemy.domain.stock.entity.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.stockdemy.domain.stock.entity.QStock.stock;
import static com.stockdemy.domain.stock.entity.QUserFavoriteStock.userFavoriteStock;

@Repository
@RequiredArgsConstructor
public class StockQueryRepository {

  private final JPAQueryFactory queryFactory;

  /**
   * 조건에 맞는 종목 조회
   *
   * <p>정렬 기준(거래량·거래대금)이 Redis 실시간 시세라 DB에서는 필터만 하고, 정렬과 페이징은
   * 서비스에서 한다. 추적 종목이 100개 수준이라 전체를 메모리에 올려도 부담이 없다는 전제다.
   */
  public List<Stock> search(String market, String sector, String keyword, Long favoriteUserId) {

    return queryFactory.selectFrom(stock)
      .where(
        eqMarket(market),
        eqSector(sector),
        containsKeyword(keyword),
        favoriteOf(favoriteUserId)
      )
      .fetch();
  }

  private BooleanExpression eqMarket(String market) {
    return StringUtils.hasText(market) ? stock.market.eq(market) : null;
  }

  private BooleanExpression eqSector(String sector) {
    return StringUtils.hasText(sector) ? stock.sector.eq(sector) : null;
  }

  // 종목명 또는 종목코드 부분 일치
  private BooleanExpression containsKeyword(String keyword) {

    if (!StringUtils.hasText(keyword)) return null;

    return stock.stockName.containsIgnoreCase(keyword).or(stock.stockCode.containsIgnoreCase(keyword));
  }

  // 사용자 관심종목만
  private BooleanExpression favoriteOf(Long userId) {

    if (userId == null) return null;

    return stock.stockCode.in(
      JPAExpressions.select(userFavoriteStock.stockCode)
        .from(userFavoriteStock)
        .where(userFavoriteStock.userId.eq(userId))
    );
  }
}
