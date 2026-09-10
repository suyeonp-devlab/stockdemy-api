package com.stockdemy.global.util;

/** 숫자 관련 공통 유틸 */
public final class NumberUtil {

  private NumberUtil() {
  }

  // 소수 둘째 자리 반올림 (앱이 가격·등락률을 받은 그대로 출력하므로 Yahoo 부동소수 오차를 정리)
  public static Double round2(Double value) {
    return value == null ? null : Math.round(value * 100) / 100.0;
  }
}
