package com.stockdemy.domain.stock.scheduler;

import com.stockdemy.domain.stock.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 종목 AI 분석 배치 (하루 1회, 뉴스 수집 이후라 새 뉴스 요약이 입력에 반영된다) */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAnalysisScheduler {

  private final StockAnalysisService stockAnalysisService;

  @Scheduled(cron = "${schedule.stock-analysis.cron}", zone = "Asia/Seoul")
  public void analyze() {

    int analyzed = stockAnalysisService.analyzeAll();

    log.info("종목 AI 분석 완료: {}건", analyzed);
  }
}
