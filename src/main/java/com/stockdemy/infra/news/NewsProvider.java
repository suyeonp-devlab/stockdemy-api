package com.stockdemy.infra.news;

import com.stockdemy.infra.news.dto.NewsArticleItem;

import java.util.List;

public interface NewsProvider {

  // 최근 기사 조회 (실패한 출처는 제외하고 반환)
  List<NewsArticleItem> fetchRecent();
}
