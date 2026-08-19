package com.stockdemy.infra.mail;

public interface MailSender {
  void sendVerificationCode(String email, String code);
}
