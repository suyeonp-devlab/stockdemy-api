package com.stockdemy.infra.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("local")
@Component
public class LoggingMailSender implements MailSender {

  @Override
  public void sendVerificationCode(String email, String code) {
    log.info("[인증코드 발송] to={}, code={}", email, code);
  }
}
