package com.stockdemy.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record SignupRequest(
  @NotBlank(message = "이메일을 입력해주세요.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  String email,

  @NotBlank(message = "비밀번호를 입력해주세요.")
  @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
  @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).*$", message = "비밀번호는 영문과 숫자를 포함해야 합니다.")
  String password,

  @NotBlank(message = "비밀번호 확인을 입력해주세요.")
  String passwordConfirm
) {

  public boolean isPasswordValid() {
    return StringUtils.hasText(password) && password.equals(passwordConfirm);
  }
}
