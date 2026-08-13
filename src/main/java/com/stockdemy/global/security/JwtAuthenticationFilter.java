package com.stockdemy.global.security;

import com.stockdemy.global.security.dto.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** SecurityContext에 인증 정보를 채워넣는 필터 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtProvider jwtProvider;
  private final TokenVersionStore tokenVersionStore;

  @Override
  @NullMarked
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {

    String token = resolveToken(request);

    if (token != null) {
      try {
        Claims claims = jwtProvider.parse(token);
        Long userId = jwtProvider.getUserId(claims);
        int tokenVersion = jwtProvider.getTokenVersion(claims);
        String email = jwtProvider.getEmail(claims);

        // 토큰버전 확인 (버전 불일치 = 무효화된 토큰)
        if (tokenVersion == tokenVersionStore.getCurrentVersion(userId)) {
          AuthUser authUser = new AuthUser(userId, email);
          var authentication = new UsernamePasswordAuthenticationToken(authUser, null, List.of());
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      } catch (JwtException | IllegalArgumentException e) {
        // 서명 위조/만료/형식 오류 — 인증 미처리 상태로 통과
      }
    }

    filterChain.doFilter(request, response);
  }

  // 토큰 추출
  private String resolveToken(HttpServletRequest request) {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      return header.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
