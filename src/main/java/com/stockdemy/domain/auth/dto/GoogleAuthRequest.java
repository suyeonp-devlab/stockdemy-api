package com.stockdemy.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
  @NotBlank(message = "accessToken이 필요합니다.")
  String accessToken) {
}
