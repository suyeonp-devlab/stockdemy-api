package com.stockdemy.global.security.dto;

/** 현재 로그인한 사용자 정보 - @AuthenticationPrincipal 사용 */
public record AuthUser(Long userId, String email) {
}
