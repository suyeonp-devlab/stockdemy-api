package com.stockdemy.domain.news.service;

import com.stockdemy.domain.news.entity.News;
import com.stockdemy.domain.news.entity.NewsRelatedStock;
import com.stockdemy.domain.news.repository.NewsRelatedStockRepository;
import com.stockdemy.domain.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 수집 뉴스 저장
 *
 * <p>수집 서비스는 외부 호출이 길어 트랜잭션을 두지 않으므로, 뉴스와 관련 종목 저장만 이 빈에서
 * 한 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class NewsSaveService {

  private final NewsRepository newsRepository;
  private final NewsRelatedStockRepository newsRelatedStockRepository;

  @Transactional
  public void save(News news, List<RelatedStock> relatedStocks) {

    News saved = newsRepository.save(news);

    if (relatedStocks.isEmpty()) return;

    newsRelatedStockRepository.saveAll(relatedStocks.stream()
      .map(related -> NewsRelatedStock.builder()
        .newsId(saved.getNewsId())
        .stockCode(related.stockCode())
        .impact(related.impact())
        .build())
      .toList());
  }

  public record RelatedStock(String stockCode, String impact) {
  }
}
