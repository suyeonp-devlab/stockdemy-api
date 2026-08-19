package com.stockdemy.domain.user.dto;

import com.stockdemy.domain.user.entity.User;
import com.stockdemy.global.util.DateTimeUtil;
import lombok.Builder;

@Builder
public record MeResponse(String email, String createdAt) {

  public static MeResponse from(User user) {
    return MeResponse.builder()
      .email(user.getEmail())
      .createdAt(DateTimeUtil.toCompactDateTime(user.getCreatedAt()))
      .build();
  }
}
