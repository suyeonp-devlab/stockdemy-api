package com.stockdemy.infra.oauth;

public interface GoogleOAuthClient {
  String verifyAndGetEmail(String accessToken);
}
