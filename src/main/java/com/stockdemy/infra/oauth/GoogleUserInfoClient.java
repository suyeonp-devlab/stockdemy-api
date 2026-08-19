package com.stockdemy.infra.oauth;

import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GoogleUserInfoClient implements GoogleOAuthClient {

  private static final String BASE_URL = "https://www.googleapis.com";
  private static final String USERINFO_PATH = "/oauth2/v3/userinfo";

  private final RestClient restClient = RestClient.builder().baseUrl(BASE_URL).build();

  // 구글 accessToken 검증
  @Override
  public String verifyAndGetEmail(String accessToken) {

    try {
      GoogleUserInfoResponse response = restClient.get()
        .uri(USERINFO_PATH)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .body(GoogleUserInfoResponse.class);

      if (response == null || !StringUtils.hasText(response.email())) {
        throw new CustomException(ErrorCode.GOOGLE_AUTH_FAILED);
      }
      return response.email();

    } catch (RestClientException e) {
      log.error("Google 메일 조회 실패", e);
      throw new CustomException(ErrorCode.GOOGLE_AUTH_FAILED);
    }
  }

  // 구글 연동 응답값
  private record GoogleUserInfoResponse(String email) {
  }
}
