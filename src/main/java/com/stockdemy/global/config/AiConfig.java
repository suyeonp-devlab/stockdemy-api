package com.stockdemy.global.config;

import com.stockdemy.infra.ai.AiClient;
import com.stockdemy.infra.ai.AiQuotaStore;
import com.stockdemy.infra.ai.AiUsage;
import com.stockdemy.infra.ai.GeminiAiClient;
import com.stockdemy.infra.ai.QuotaAwareAiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Configuration
public class AiConfig {

  // 무료 티어 한도를 뉴스 분석·종목 분석·일지 복기가 나눠 쓰므로 호출을 한 곳에서 용도별로 제한한다
  @Bean
  public AiClient aiClient(
    @Value("${ai.gemini.api-key:}") String apiKey,
    @Value("${ai.gemini.model}") String model,
    @Value("${ai.daily-limit.news}") int newsLimit,
    @Value("${ai.daily-limit.stock}") int stockLimit,
    @Value("${ai.daily-limit.journal}") int journalLimit,
    ObjectMapper objectMapper,
    AiQuotaStore aiQuotaStore
  ) {
    Map<AiUsage, Integer> dailyLimits = Map.of(
      AiUsage.NEWS, newsLimit,
      AiUsage.STOCK, stockLimit,
      AiUsage.JOURNAL, journalLimit
    );

    return new QuotaAwareAiClient(new GeminiAiClient(apiKey, model, objectMapper), aiQuotaStore, dailyLimits);
  }
}
