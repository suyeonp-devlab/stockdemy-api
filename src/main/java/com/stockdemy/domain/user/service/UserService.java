package com.stockdemy.domain.user.service;

import com.stockdemy.domain.auth.dto.TokenItem;
import com.stockdemy.domain.user.dto.ChangePasswordRequest;
import com.stockdemy.domain.user.dto.MeResponse;
import com.stockdemy.domain.user.dto.WithdrawRequest;
import com.stockdemy.domain.user.entity.User;
import com.stockdemy.domain.user.repository.UserRepository;
import com.stockdemy.global.exception.CustomException;
import com.stockdemy.global.exception.ErrorCode;
import com.stockdemy.global.security.JwtProvider;
import com.stockdemy.global.security.RefreshTokenStore;
import com.stockdemy.global.security.TokenVersionStore;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;
  private final RefreshTokenStore refreshTokenStore;
  private final TokenVersionStore tokenVersionStore;

  // 내 정보 조회
  public MeResponse getMe(Long userId) {
    User user = userRepository.findById(userId)
      .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    return MeResponse.from(user);
  }

  // 비밀번호 변경
  @Transactional
  public TokenItem changePassword(Long userId, ChangePasswordRequest request) {

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

    if (!request.isNewPasswordValid()) {
      throw new CustomException(ErrorCode.NEW_PASSWORD_MISMATCH);
    }

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
    }

    user.changePassword(passwordEncoder.encode(request.newPassword()));
    tokenVersionStore.refresh(userId, user.getTokenVersion());
    refreshTokenStore.revokeAll(userId); // 모든 세션 종료

    // 현재 세션 유지용 토큰 발급
    String accessToken = jwtProvider.createAccessToken(userId, user.getEmail(), user.getTokenVersion());
    String refreshToken = refreshTokenStore.create(userId);
    return new TokenItem(accessToken, refreshToken);
  }

  // 회원 탈퇴
  @Transactional
  public void withdraw(Long userId, @Nullable WithdrawRequest request) {

    User user = userRepository.findById(userId)
      .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

    if (!user.isGoogleUser()) {
      if (request == null) throw new CustomException(ErrorCode.INVALID_REQUEST);
      if (!passwordEncoder.matches(request.password(), user.getPassword())) {
        throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
      }
    }

    // 모든 세션 종료 후 삭제
    user.withdraw();
    tokenVersionStore.refresh(userId, user.getTokenVersion());
    refreshTokenStore.revokeAll(userId);
  }
}
