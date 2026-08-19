package com.stockdemy.domain.auth.service;

import com.stockdemy.domain.auth.dto.*;
import com.stockdemy.domain.auth.store.VerificationCodeStore;
import com.stockdemy.domain.auth.store.VerificationPurpose;
import com.stockdemy.domain.user.entity.User;
import com.stockdemy.domain.user.repository.UserRepository;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import com.stockdemy.global.security.JwtProvider;
import com.stockdemy.global.security.RateLimiter;
import com.stockdemy.global.security.RefreshTokenStore;
import com.stockdemy.global.security.TokenVersionStore;
import java.time.Duration;

import com.stockdemy.global.security.dto.RefreshLookupResult;
import com.stockdemy.infra.mail.MailSender;
import com.stockdemy.infra.oauth.GoogleOAuthClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;
  private final RefreshTokenStore refreshTokenStore;
  private final TokenVersionStore tokenVersionStore;
  private final VerificationCodeStore verificationCodeStore;
  private final MailSender mailSender;
  private final GoogleOAuthClient googleOAuthClient;
  private final RateLimiter rateLimiter;

  private static final Duration SEND_CODE_COOLDOWN = Duration.ofSeconds(60);   // 연속 발송 최소 간격
  private static final int SEND_CODE_MAX_PER_HOUR = 5;                         // 시간당 최대 발송 횟수
  private static final Duration SEND_CODE_WINDOW = Duration.ofHours(1);        // 발송 횟수 집계 기준 시간
  private static final int LOGIN_FAIL_LIMIT = 5;                               // 로그인 실패 허용 횟수

  // 회원가입 인증코드 발송
  public void sendSignupCode(SendCodeRequest request) {

    if (userRepository.existsByEmail(request.email())) {
      throw new CustomException(ErrorCode.EMAIL_DUPLICATE);
    }

    enforceSendCodeLimit(VerificationPurpose.SIGNUP, request.email());

    String code = verificationCodeStore.issue(VerificationPurpose.SIGNUP, request.email());
    mailSender.sendVerificationCode(request.email(), code);
  }

  // 회원가입 인증코드 검증
  public void verifySignupCode(VerifyCodeRequest request) {
    verificationCodeStore.verify(VerificationPurpose.SIGNUP, request.email(), request.code());
  }

  // 회원가입
  @Transactional
  public TokenItem signup(SignupRequest request) {

    if (!request.isPasswordValid()) {
      throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
    }

    if (userRepository.existsByEmail(request.email())) {
      throw new CustomException(ErrorCode.EMAIL_DUPLICATE);
    }

    verificationCodeStore.requireVerified(VerificationPurpose.SIGNUP, request.email());

    User user = User.createLocalUser(request.email(), passwordEncoder.encode(request.password()));
    userRepository.save(user);

    return issueTokens(user);
  }

  // 비밀번호 재설정 인증코드 발송
  public void sendPasswordResetCode(SendCodeRequest request) {

    if (!userRepository.existsByEmail(request.email())) {
      throw new CustomException(ErrorCode.EMAIL_NOT_FOUND);
    }

    enforceSendCodeLimit(VerificationPurpose.PASSWORD_RESET, request.email());

    String code = verificationCodeStore.issue(VerificationPurpose.PASSWORD_RESET, request.email());
    mailSender.sendVerificationCode(request.email(), code);
  }

  // 비밀번호 재설정 인증코드 검증
  public void verifyPasswordResetCode(VerifyCodeRequest request) {
    verificationCodeStore.verify(VerificationPurpose.PASSWORD_RESET, request.email(), request.code());
  }

  // 비밀번호 재설정
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {

    if (!request.isPasswordValid()) {
      throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
    }

    User user = userRepository.findByEmail(request.email())
      .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND));

    verificationCodeStore.requireVerified(VerificationPurpose.PASSWORD_RESET, request.email());

    user.changePassword(passwordEncoder.encode(request.password()));
    tokenVersionStore.refresh(user.getUserId(), user.getTokenVersion());
    refreshTokenStore.revokeAll(user.getUserId()); // 모든 세션 종료
  }

  // 로그인
  @Transactional(noRollbackFor = CustomException.class)
  public TokenItem login(LoginRequest request) {

    User user = userRepository.findByEmail(request.email())
      .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

    if (user.getFailedLoginAttempts() >= LOGIN_FAIL_LIMIT) {
      throw new CustomException(ErrorCode.PASSWORD_RESET_REQUIRED);
    }

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      user.recordLoginFailure();
      if (user.getFailedLoginAttempts() >= LOGIN_FAIL_LIMIT) {
        throw new CustomException(ErrorCode.PASSWORD_RESET_REQUIRED);
      }
      throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
    }

    user.recordLoginSuccess();
    return issueTokens(user);
  }

  // 구글 로그인/회원가입 (없으면 가입, 있으면 로그인)
  @Transactional
  public TokenItem loginOrSignupWithGoogle(GoogleAuthRequest request) {

    String email = googleOAuthClient.verifyAndGetEmail(request.accessToken());

    User user = userRepository.findByEmail(email)
      .orElseGet(() -> userRepository.save(User.createGoogleUser(email)));

    return issueTokens(user);
  }

  // 액세스 토큰 재발급
  @Transactional(noRollbackFor = CustomException.class)
  public TokenItem refresh(String refreshToken) {

    if (refreshToken == null) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    RefreshLookupResult result = refreshTokenStore.lookup(refreshToken);

    if (result.status() == RefreshLookupResult.Status.REUSED) {
      // 이미 폐기된 토큰 재사용 > 탈취 의심 (모든 세션 강제 종료)
      forceLogoutAllSessions(result.userId());
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    if (result.status() == RefreshLookupResult.Status.NOT_FOUND) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    // 현재 세션 폐기 후 token 재발급
    Long userId = result.userId();
    refreshTokenStore.markRotated(refreshToken, userId);

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED));

    if (user.isWithdrawn()) {
      throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    return issueTokens(user);
  }

  // 로그아웃
  public void logout(String refreshToken) {

    if (refreshToken == null) return;

    // 현재 로그인 세션 무효화
    RefreshLookupResult result = refreshTokenStore.lookup(refreshToken);
    if (result.status() == RefreshLookupResult.Status.VALID) {
      refreshTokenStore.revokeOne(refreshToken, result.userId());
    }
  }

  // 인증코드 발송 제한여부 확인
  private void enforceSendCodeLimit(VerificationPurpose purpose, String email) {

    // 쿨타임
    String cooldownKey = "cooldown:code-send:" + purpose + ":" + email;
    if (!rateLimiter.tryAcquireCooldown(cooldownKey, SEND_CODE_COOLDOWN)) {
      long remaining = rateLimiter.getRemainingSeconds(cooldownKey);
      throw new CustomException(ErrorCode.COOLDOWN, remaining + "초 후 다시 시도해주세요.");
    }

    // 기간 한도
    String rateLimitKey = "ratelimit:code-send:" + purpose + ":" + email;
    if (!rateLimiter.tryConsume(rateLimitKey, SEND_CODE_MAX_PER_HOUR, SEND_CODE_WINDOW)) {
      throw new CustomException(ErrorCode.RATE_LIMIT_EXCEEDED, "인증코드 발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.");
    }
  }

  // token 발급
  private TokenItem issueTokens(User user) {
    String accessToken = jwtProvider.createAccessToken(user.getUserId(), user.getEmail(), user.getTokenVersion());
    String refreshToken = refreshTokenStore.create(user.getUserId());
    return new TokenItem(accessToken, refreshToken);
  }

  // 사용자 모든 세션 강제 종료
  private void forceLogoutAllSessions(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.increaseTokenVersion();
    tokenVersionStore.refresh(userId, user.getTokenVersion());
    refreshTokenStore.revokeAll(userId);
  }
}
