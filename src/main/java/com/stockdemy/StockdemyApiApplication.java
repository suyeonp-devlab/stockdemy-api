package com.stockdemy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class StockdemyApiApplication {

  // 배포 환경(컨테이너 기본 UTC 등)과 무관하게 서버 시각 기준을 고정
  private static final String DEFAULT_TIME_ZONE = "Asia/Seoul";

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
    SpringApplication.run(StockdemyApiApplication.class, args);
  }
}
