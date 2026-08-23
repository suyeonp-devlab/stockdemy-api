package com.stockdemy.global.config;

import com.stockdemy.infra.ai.AiClient;
import com.stockdemy.infra.ai.GeminiAiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class AiConfig {

  @Bean
  public AiClient aiClient(
    @Value("${ai.gemini.api-key:}") String apiKey,
    @Value("${ai.gemini.model}") String model,
    ObjectMapper objectMapper
  ) {
    return new GeminiAiClient(apiKey, model, objectMapper);
  }
}
