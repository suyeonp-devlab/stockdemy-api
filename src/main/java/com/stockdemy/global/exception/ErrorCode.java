package com.stockdemy.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  // 공통
  NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리소스입니다."),
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
  SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다.\n잠시 후 다시 시도해 주세요."),

  // 이메일 인증코드
  CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다."),
  CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증코드가 만료되었습니다."),
  CODE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."),
  MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다."),

  // 인증
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
  PASSWORD_RESET_REQUIRED(HttpStatus.LOCKED, "로그인 실패 횟수를 초과했습니다. 비밀번호를 재설정해주세요."),
  REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해주세요."),
  GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "구글 인증에 실패했습니다."),
  PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

  // 회원
  EMAIL_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 가입된 이메일입니다."),
  EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "가입되지 않은 이메일입니다."),

  // 요청 제한
  COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요."),
  RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");

  private final HttpStatus status;
  private final String defaultMessage;

  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }
}
