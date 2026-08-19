package com.stockdemy.infra.mail;

import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Profile("!local")
@Component
public class ResendMailSender implements MailSender {

  private static final String BASE_URL = "https://api.resend.com";
  private static final String EMAILS_PATH = "/emails";

  private final RestClient restClient;
  private final String fromAddress;

  public ResendMailSender(
    @Value("${resend.api-key}") String apiKey,
    @Value("${resend.from-address}") String fromAddress
  ) {
    this.restClient = RestClient.builder().baseUrl(BASE_URL)
      .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).build();
    this.fromAddress = fromAddress;
  }

  @Override
  public void sendVerificationCode(String email, String code) {

    try {
      restClient.post()
        .uri(EMAILS_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new EmailRequest(fromAddress, List.of(email), "[Stockdemy] 인증코드", "인증코드: " + code))
        .retrieve()
        .toBodilessEntity();

    } catch (RestClientException e) {
      log.error("Resend 메일 발송 실패", e);
      throw new CustomException(ErrorCode.MAIL_SEND_FAILED);
    }
  }

  // resend 연동 요청값
  private record EmailRequest(String from, List<String> to, String subject, String text) {
  }
}
