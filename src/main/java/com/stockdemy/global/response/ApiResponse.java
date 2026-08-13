package com.stockdemy.global.response;

/** 서버 응답 공통 포맷 */
public record ApiResponse<T>(
  boolean success,   // 성공여부
  String code,       // 코드
  String message,    // 메세지
  T data             // 데이터
) {

  // 조회 성공 응답
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, "OK", "조회 성공", data);
  }

  // 메세지를 직접 지정하는 성공 응답
  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, "OK", message, data);
  }

  // 실패 응답
  public static <T> ApiResponse<T> fail(String code, String message) {
    return new ApiResponse<>(false, code, message, null);
  }
}
