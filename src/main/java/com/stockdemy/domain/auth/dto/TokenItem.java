package com.stockdemy.domain.auth.dto;

public record TokenItem(
  String accessToken,
  String refreshToken) {
}
