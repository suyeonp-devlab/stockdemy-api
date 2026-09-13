package com.stockdemy.global.init;

import com.stockdemy.domain.news.repository.NewsRepository;
import com.stockdemy.domain.news.service.NewsCollectService;
import com.stockdemy.domain.stock.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * AI 배치 초기 실행 러너
 *
 * <p>다음 배치 시각까지 화면이 비지 않도록 기동 시 뉴스 수집 → 종목 분석 순서로 한 번 돌린다. 종목 분석이
 * 새 뉴스 요약을 입력으로 쓰므로 한 스레드에서 순서대로 실행한다. 재기동마다 AI 한도를 쓰지 않도록 최근
 * 이력이 있으면 각각 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBatchRunner implements ApplicationRunner {

  private final NewsRepository newsRepository;
  private final NewsCollectService newsCollectService;
  private final StockAnalysisService stockAnalysisService;

  @Value("${news.recent-hours}")
  private int recentHours;

  @Async
  @Override
  public void run(ApplicationArguments args) {
    collectNews();
    analyzeStocks();
  }

  // 뉴스 초기 수집
  private void collectNews() {

    if (newsRepository.existsByCreatedAtAfter(LocalDateTime.now().minusHours(recentHours))) {
      log.info("뉴스 초기 수집 생략: 최근 {}시간 내 수집 이력 있음", recentHours);
      return;
    }

    log.info("뉴스 초기 수집 시작");
    log.info("뉴스 초기 수집 완료: {}건 저장", newsCollectService.collectAll());
  }

  // 종목 초기 분석
  private void analyzeStocks() {

    if (stockAnalysisService.analyzedToday()) {
      log.info("종목 초기 분석 생략: 오늘 분석 이력 있음");
      return;
    }

    log.info("종목 초기 분석 시작");
    log.info("종목 초기 분석 완료: {}건", stockAnalysisService.analyzeAll());
  }
}
