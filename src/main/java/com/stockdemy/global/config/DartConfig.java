package com.stockdemy.global.config;

import com.stockdemy.infra.dart.DartDisclosureProvider;
import com.stockdemy.infra.dart.DisclosureProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DartConfig {

  @Bean
  public DisclosureProvider disclosureProvider(@Value("${dart.api-key:}") String apiKey) {
    return new DartDisclosureProvider(apiKey);
  }
}
