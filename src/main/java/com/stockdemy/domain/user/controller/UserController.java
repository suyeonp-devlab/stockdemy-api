package com.stockdemy.domain.user.controller;

import com.stockdemy.domain.auth.dto.AuthTokenResponse;
import com.stockdemy.domain.auth.dto.TokenItem;
import com.stockdemy.domain.auth.service.AuthService;
import com.stockdemy.domain.user.dto.ChangePasswordRequest;
import com.stockdemy.domain.user.dto.MeResponse;
import com.stockdemy.domain.user.dto.WithdrawRequest;
import com.stockdemy.domain.user.service.UserService;
import com.stockdemy.global.response.ApiResponse;
import com.stockdemy.global.security.annotation.CurrentUserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "내 정보 조회/수정 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final AuthService authService;

  @Operation(summary = "내 정보 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<MeResponse>> getMe(@CurrentUserId Long userId) {
    MeResponse me = userService.getMe(userId);
    return ResponseEntity.ok(ApiResponse.success(me));
  }

  @Operation(summary = "비밀번호 변경")
  @PutMapping("/password")
  public ResponseEntity<ApiResponse<AuthTokenResponse>> changePassword(
    @CurrentUserId Long userId,
    @Valid @RequestBody ChangePasswordRequest request
  ) {
    TokenItem tokens = userService.changePassword(userId, request);
    return authService.withRefreshCookie(tokens, "비밀번호가 변경되었습니다.");
  }

  @Operation(summary = "회원 탈퇴")
  @DeleteMapping
  public ResponseEntity<ApiResponse<Void>> withdraw(
    @CurrentUserId Long userId,
    @Valid @RequestBody WithdrawRequest request
  ) {
    userService.withdraw(userId, request);
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, authService.expiredRefreshTokenCookie().toString())
      .body(ApiResponse.success("탈퇴 처리 되었습니다.", null));
  }

  @Operation(summary = "구글 회원 탈퇴")
  @DeleteMapping("/google")
  public ResponseEntity<ApiResponse<Void>> withdrawWithGoogle(@CurrentUserId Long userId) {
    userService.withdraw(userId, null);
    return ResponseEntity.ok()
      .header(HttpHeaders.SET_COOKIE, authService.expiredRefreshTokenCookie().toString())
      .body(ApiResponse.success("탈퇴 처리 되었습니다.", null));
  }
}
