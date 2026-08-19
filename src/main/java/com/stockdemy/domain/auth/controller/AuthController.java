package com.stockdemy.domain.auth.controller;

import com.stockdemy.domain.auth.dto.*;
import com.stockdemy.domain.auth.service.AuthService;
import com.stockdemy.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.stockdemy.domain.auth.service.AuthService.REFRESH_TOKEN_COOKIE;

@Tag(name = "인증", description = "회원가입/로그인 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "회원가입 인증코드 발송")
  @PostMapping("/signup/code/send")
  public ResponseEntity<ApiResponse<Void>> sendSignupCode(@Valid @RequestBody SendCodeRequest request) {
    authService.sendSignupCode(request);
    return ResponseEntity.ok(ApiResponse.success("인증코드가 발송되었습니다.", null));
  }

  @Operation(summary = "회원가입 인증코드 검증")
  @PostMapping("/signup/code/verify")
  public ResponseEntity<ApiResponse<Void>> verifySignupCode(@Valid @RequestBody VerifyCodeRequest request) {
    authService.verifySignupCode(request);
    return ResponseEntity.ok(ApiResponse.success("인증이 완료되었습니다.", null));
  }

  @Operation(summary = "회원가입")
  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<AuthTokenResponse>> signup(@Valid @RequestBody SignupRequest request) {
    TokenItem tokens = authService.signup(request);
    return authService.withRefreshCookie(tokens, "회원가입이 완료되었습니다.");
  }

  @Operation(summary = "구글 회원가입")
  @PostMapping("/signup/google")
  public ResponseEntity<ApiResponse<AuthTokenResponse>> signupWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
    TokenItem tokens = authService.loginOrSignupWithGoogle(request);
    return authService.withRefreshCookie(tokens, "구글 회원가입이 완료되었습니다.");
  }

  @Operation(summary = "로그인")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthTokenResponse>> login(@Valid @RequestBody LoginRequest request) {
    TokenItem tokens = authService.login(request);
    return authService.withRefreshCookie(tokens, "로그인 되었습니다.");
  }

  @Operation(summary = "구글 로그인")
  @PostMapping("/login/google")
  public ResponseEntity<ApiResponse<AuthTokenResponse>> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
    TokenItem tokens = authService.loginOrSignupWithGoogle(request);
    return authService.withRefreshCookie(tokens, "구글 로그인이 완료되었습니다.");
  }

  @Operation(summary = "비밀번호 재설정 인증코드 발송")
  @PostMapping("/password/code/send")
  public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(@Valid @RequestBody SendCodeRequest request) {
    authService.sendPasswordResetCode(request);
    return ResponseEntity.ok(ApiResponse.success("인증코드가 발송되었습니다.", null));
  }

  @Operation(summary = "비밀번호 재설정 인증코드 검증")
  @PostMapping("/password/code/verify")
  public ResponseEntity<ApiResponse<Void>> verifyPasswordResetCode(@Valid @RequestBody VerifyCodeRequest request) {
    authService.verifyPasswordResetCode(request);
    return ResponseEntity.ok(ApiResponse.success("인증이 완료되었습니다.", null));
  }

  @Operation(summary = "비밀번호 재설정")
  @PostMapping("/password/reset")
  public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ResponseEntity.ok(ApiResponse.success("비밀번호가 재설정되었습니다.", null));
  }

  @Operation(summary = "액세스 토큰 재발급")
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
    @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
  ) {
    TokenItem tokens = authService.refresh(refreshToken);
    return authService.withRefreshCookie(tokens, "토큰이 갱신되었습니다.");
  }

  @Operation(summary = "로그아웃")
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
    @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken
  ) {
    authService.logout(refreshToken);
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, authService.expiredRefreshTokenCookie().toString())
      .body(ApiResponse.success("로그아웃 되었습니다.", null));
  }

}
