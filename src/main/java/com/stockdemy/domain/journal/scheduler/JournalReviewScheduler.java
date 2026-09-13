package com.stockdemy.domain.journal.scheduler;

import com.stockdemy.domain.journal.service.JournalReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 일지 AI 복기 대기열 처리 (주기 실행, 기동 직후 한 번 돌아 재기동 전 남은 요청을 이어서 처리) */
@Slf4j
@Component
@RequiredArgsConstructor
public class JournalReviewScheduler {

  private final JournalReviewService journalReviewService;

  @Scheduled(
    fixedDelayString = "${journal.review-interval-ms}",
    initialDelayString = "${journal.review-initial-delay-ms}"
  )
  public void review() {

    int completed = journalReviewService.reviewPending();

    if (completed > 0) {
      log.info("일지 AI 복기 완료: {}건", completed);
    }
  }
}
