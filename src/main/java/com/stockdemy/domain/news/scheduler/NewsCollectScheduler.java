package com.stockdemy.domain.news.scheduler;

import com.stockdemy.domain.news.service.NewsCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 뉴스 수집 배치 (하루 1회) */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectScheduler {

  private final NewsCollectService newsCollectService;

  @Scheduled(cron = "${schedule.news.cron}", zone = "Asia/Seoul")
  public void collect() {

    int saved = newsCollectService.collectAll();

    log.info("뉴스 수집 완료: {}건 저장", saved);
  }
}
