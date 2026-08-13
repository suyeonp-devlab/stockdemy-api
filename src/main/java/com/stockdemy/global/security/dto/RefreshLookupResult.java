package com.stockdemy.global.security.dto;

/** refresh token 조회 결과 */
public record RefreshLookupResult(Status status, Long userId) {

  // VALID: 정상 토큰
  // REUSED: 폐기된 토큰 (재사용 시 탈취 의심)
  // NOT_FOUND: 없음 (만료)
  public enum Status { VALID, REUSED, NOT_FOUND }

  public static RefreshLookupResult valid(Long userId) {
    return new RefreshLookupResult(Status.VALID, userId);
  }

  public static RefreshLookupResult reused(Long userId) {
    return new RefreshLookupResult(Status.REUSED, userId);
  }

  public static RefreshLookupResult notFound() {
    return new RefreshLookupResult(Status.NOT_FOUND, null);
  }
}
