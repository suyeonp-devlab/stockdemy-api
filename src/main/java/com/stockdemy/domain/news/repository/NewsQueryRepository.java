package com.stockdemy.domain.news.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.stockdemy.domain.news.entity.News;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.stockdemy.domain.news.entity.QNews.news;
import static com.stockdemy.domain.news.entity.QNewsRelatedStock.newsRelatedStock;
import static com.stockdemy.domain.stock.entity.QStock.stock;
import static com.stockdemy.domain.stock.entity.QUserFavoriteStock.userFavoriteStock;

@Repository
@RequiredArgsConstructor
public class NewsQueryRepository {

  private final JPAQueryFactory queryFactory;

  // 조건에 맞는 뉴스 페이지 조회 (최신 발행순)
  public List<News> search(String category, String keyword, Long favoriteUserId, int offset, int limit) {

    return queryFactory.selectFrom(news)
      .where(conditions(category, keyword, favoriteUserId))
      .orderBy(news.publishedAt.desc(), news.newsId.desc())
      .offset(offset)
      .limit(limit)
      .fetch();
  }

  // 조건에 맞는 뉴스 건수
  public long count(String category, String keyword, Long favoriteUserId) {

    Long count = queryFactory.select(news.count())
      .from(news)
      .where(conditions(category, keyword, favoriteUserId))
      .fetchOne();

    return count == null ? 0 : count;
  }

  private BooleanExpression[] conditions(String category, String keyword, Long favoriteUserId) {
    return new BooleanExpression[] {eqCategory(category), matchesStock(keyword), favoriteOf(favoriteUserId)};
  }

  private BooleanExpression eqCategory(String category) {
    return StringUtils.hasText(category) ? news.category.eq(category) : null;
  }

  // 종목명 또는 종목코드 부분 일치 (많이 언급된 종목 건수와 맞추기 위해 관련 종목으로 엮인 뉴스도 포함)
  private BooleanExpression matchesStock(String keyword) {

    if (!StringUtils.hasText(keyword)) return null;

    BooleanExpression asMainStock = news.stockCode.in(matchedStockCodes(keyword));

    BooleanExpression asRelatedStock = news.newsId.in(
      JPAExpressions.select(newsRelatedStock.newsId)
        .from(newsRelatedStock)
        .where(newsRelatedStock.stockCode.in(matchedStockCodes(keyword)))
    );

    return asMainStock.or(asRelatedStock);
  }

  private JPQLQuery<String> matchedStockCodes(String keyword) {
    return JPAExpressions.select(stock.stockCode)
      .from(stock)
      .where(stock.stockName.containsIgnoreCase(keyword).or(stock.stockCode.containsIgnoreCase(keyword)));
  }

  // 사용자 관심종목 뉴스만
  private BooleanExpression favoriteOf(Long userId) {

    if (userId == null) return null;

    return news.stockCode.in(
      JPAExpressions.select(userFavoriteStock.stockCode)
        .from(userFavoriteStock)
        .where(userFavoriteStock.userId.eq(userId))
    );
  }
}
