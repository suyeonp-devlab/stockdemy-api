package com.stockdemy.global.init;

import com.stockdemy.domain.news.repository.NewsRepository;
import com.stockdemy.domain.news.service.NewsCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 뉴스 초기 수집 러너
 *
 * <p>다음 배치 시각까지 화면이 비어 있지 않도록 기동 시 한 번 수집한다. 재기동마다 AI 호출을
 * 쓰지 않도록 최근 수집 이력이 있으면 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectRunner implements ApplicationRunner {

  private final NewsRepository newsRepository;
  private final NewsCollectService newsCollectService;

  @Value("${news.recent-hours}")
  private int recentHours;

  @Async
  @Override
  public void run(ApplicationArguments args) {

    if (newsRepository.existsByCreatedAtAfter(LocalDateTime.now().minusHours(recentHours))) {
      log.info("뉴스 초기 수집 생략: 최근 {}시간 내 수집 이력 있음", recentHours);
      return;
    }

    log.info("뉴스 초기 수집 시작");

    int saved = newsCollectService.collectAll();

    log.info("뉴스 초기 수집 완료: {}건 저장", saved);
  }
}
