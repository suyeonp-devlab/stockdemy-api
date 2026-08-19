package com.stockdemy.domain.user.entity;

import com.stockdemy.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userId;

  // 이메일
  private String email;

  // 비밀번호
  private String password;

  // 토큰 버전
  @Column(nullable = false)
  private Integer tokenVersion;

  // 회원 상태
  @Column(nullable = false, length = 20)
  private String status;

  // 비밀번호 연속 실패 횟수
  @Column(nullable = false)
  private Integer failedLoginAttempts;

  // 가입 경로
  @Column(nullable = false, length = 20)
  private String provider;

  // 탈퇴일시
  private LocalDateTime withdrawnAt;

  // 비밀번호 변경일시
  private LocalDateTime passwordChangedAt;

  // 마지막 로그인 일시
  private LocalDateTime lastLoginAt;

  @Builder
  private User(String email, String password, String provider) {
    this.email = email;
    this.password = password;
    this.tokenVersion = 0;
    this.status = "ACTIVE";
    this.failedLoginAttempts = 0;
    this.provider = provider;
    this.passwordChangedAt = LocalDateTime.now();
  }

  // 자체 회원 가입
  public static User createLocalUser(String email, String encodedPassword) {
    return User.builder()
      .email(email)
      .password(encodedPassword)
      .provider("LOCAL")
      .build();
  }

  // 구글 회원 가입
  public static User createGoogleUser(String email) {
    return User.builder()
      .email(email)
      .password(null)
      .provider("GOOGLE")
      .build();
  }

  // 비밀번호 변경
  public void changePassword(String encodedPassword) {
    this.password = encodedPassword;
    this.tokenVersion++;
    this.failedLoginAttempts = 0;
    this.passwordChangedAt = LocalDateTime.now();
  }

  // 회원 탈퇴
  public void withdraw() {
    this.email = null;
    this.tokenVersion++;
    this.status = "WITHDRAWN";
    this.withdrawnAt = LocalDateTime.now();
  }

  // 로그인 성공
  public void recordLoginSuccess() {
    this.failedLoginAttempts = 0;
    this.lastLoginAt = LocalDateTime.now();
  }

  // 로그인 실패
  public void recordLoginFailure() {
    this.failedLoginAttempts++;
  }

  // 토큰 버전 증가
  public void increaseTokenVersion() {
    this.tokenVersion++;
  }

  // 탈퇴 여부
  public boolean isWithdrawn() {
    return "WITHDRAWN".equals(status);
  }
}
