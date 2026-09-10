package com.stockdemy.global.security;

import com.stockdemy.global.security.dto.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 현재 인증 정보 조회 유틸
 *
 * <p>로그인이 선택인 공개 API용이다. 비로그인 요청의 인증 주체는 문자열 {@code "anonymousUser"}라서
 * {@code @CurrentUserId}로 받으면 오류가 난다.
 */
public final class SecurityUtil {

  private SecurityUtil() {
  }

  // 로그인 사용자 ID (비로그인·무효 토큰이면 empty)
  public static Optional<Long> findCurrentUserId() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof AuthUser authUser) {
      return Optional.ofNullable(authUser.userId());
    }

    return Optional.empty();
  }
}
