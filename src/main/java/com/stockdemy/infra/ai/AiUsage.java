package com.stockdemy.infra.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** AI 호출 용도 (용도별로 하루 한도를 나눠 배치가 사용자 요청분을 잠식하지 않게 한다) */
@Getter
@RequiredArgsConstructor
public enum AiUsage {

  NEWS("뉴스 분석"),
  STOCK("종목 분석"),
  JOURNAL("일지 복기");

  private final String label;
}
