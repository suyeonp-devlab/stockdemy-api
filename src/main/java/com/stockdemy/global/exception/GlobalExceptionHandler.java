package com.stockdemy.global.exception;

import com.stockdemy.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 비즈니스 예외
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
    ErrorCode errorCode = e.getErrorCode();
    return ResponseEntity.status(errorCode.getStatus())
      .body(ApiResponse.fail(errorCode.name(), e.getMessage()));
  }

  // @Valid 검증 실패
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
    // 첫 번째 필드 에러 메세지 노출
    String message = e.getBindingResult().getFieldErrors().stream()
      .findFirst()
      .map(FieldError::getDefaultMessage)
      .orElse(ErrorCode.INVALID_REQUEST.getDefaultMessage());

    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
      .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST.name(), message));
  }

  // 예외
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("handleException", e);

    ErrorCode errorCode = ErrorCode.SERVER_ERROR;
    return ResponseEntity.status(errorCode.getStatus())
      .body(ApiResponse.fail(errorCode.name(), e.getMessage()));
  }
}
