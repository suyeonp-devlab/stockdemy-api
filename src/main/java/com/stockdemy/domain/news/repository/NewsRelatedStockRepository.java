package com.stockdemy.domain.news.repository;

import com.stockdemy.domain.news.entity.NewsRelatedStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsRelatedStockRepository extends JpaRepository<NewsRelatedStock, Long> {

  List<NewsRelatedStock> findByNewsId(Long newsId);
}
