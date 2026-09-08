package com.stockdemy.global.config;

import com.stockdemy.infra.marketdata.DummyMarketDataProvider;
import com.stockdemy.infra.marketdata.MarketDataProvider;
import com.stockdemy.infra.marketdata.YahooMarketDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MarketDataConfig {

  @Bean
  public MarketDataProvider marketDataProvider(@Value("${market-data.provider:dummy}") String provider) {

    // 오타로 dummy가 운영에 올라가는 것을 막기 위해 알 수 없는 값은 기동 실패 처리
    return switch (provider.toLowerCase()) {
      case "yahoo" -> new YahooMarketDataProvider();
      case "dummy" -> new DummyMarketDataProvider();
      default -> throw new IllegalStateException("지원하지 않는 시세 프로바이더입니다: " + provider);
    };
  }
}
