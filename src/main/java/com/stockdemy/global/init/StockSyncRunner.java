package com.stockdemy.global.init;

import com.stockdemy.domain.stock.repository.StockRepository;
import com.stockdemy.domain.stock.service.StockBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 시세 초기 적재 러너
 *
 * <p>한 번도 동기화되지 않은 종목만 채운다. 주기 갱신 스케줄러와는 무관하며, 모든 종목이
 * 채워지면 이후 기동에서는 외부 호출이 발생하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncRunner implements ApplicationRunner {

  private static final int DAILY_BAR_DAYS = 365;

  // 연속 호출 시 차단당하지 않도록 종목 간 간격을 둔다
  private static final long CALL_INTERVAL_MS = 250;

  private final StockRepository stockRepository;
  private final StockBackfillService stockBackfillService;

  @Async
  @Override
  public void run(ApplicationArguments args) {

    List<String> targets = stockRepository.findCodesNeverSynced();

    if (targets.isEmpty()) {
      log.info("초기 시세 적재 대상 없음");
      return;
    }

    log.info("초기 시세 적재 시작: {}건", targets.size());
    int success = 0;

    for (String stockCode : targets) {

      try {
        if (stockBackfillService.backfill(stockCode, DAILY_BAR_DAYS)) {
          success++;
        }
        Thread.sleep(CALL_INTERVAL_MS);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("초기 시세 적재 중단: {}/{}건", success, targets.size());
        return;

      } catch (Exception e) {
        log.warn("초기 시세 적재 실패 (stockCode={}): {}", stockCode, e.getMessage());
      }
    }

    log.info("초기 시세 적재 완료: {}/{}건", success, targets.size());
  }
}
