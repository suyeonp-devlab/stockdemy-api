package com.stockdemy.global.config;

import com.stockdemy.infra.ai.AiClient;
import com.stockdemy.infra.ai.AiQuotaStore;
import com.stockdemy.infra.ai.GeminiAiClient;
import com.stockdemy.infra.ai.QuotaAwareAiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class AiConfig {

  // 무료 티어 한도를 뉴스 분석·종목 분석·일지 복기가 함께 쓰므로 호출을 한 곳에서 제한한다
  @Bean
  public AiClient aiClient(
    @Value("${ai.gemini.api-key:}") String apiKey,
    @Value("${ai.gemini.model}") String model,
    @Value("${ai.daily-limit}") int dailyLimit,
    ObjectMapper objectMapper,
    AiQuotaStore aiQuotaStore
  ) {
    return new QuotaAwareAiClient(new GeminiAiClient(apiKey, model, objectMapper), aiQuotaStore, dailyLimit);
  }
}
