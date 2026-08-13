package com.stockdemy.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.*;

/** Access Token(JWT) 발급 및 검증 */
@Component
public class JwtProvider {

  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_TOKEN_VERSION = "tokenVersion";

  private final SecretKey secretKey;
  private final long accessTokenValiditySeconds;

  public JwtProvider(
    @Value("${jwt.secret}") String secret,
    @Value("${jwt.access-token-validity-seconds}") long accessTokenValiditySeconds
  ) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(UTF_8));
    this.accessTokenValiditySeconds = accessTokenValiditySeconds;
  }

  // access token 생성
  public String createAccessToken(Long userId, String email, int tokenVersion) {
    Instant now = Instant.now();
    return Jwts.builder()
      .subject(String.valueOf(userId))
      .claim(CLAIM_EMAIL, email)
      .claim(CLAIM_TOKEN_VERSION, tokenVersion)
      .issuedAt(Date.from(now))
      .expiration(Date.from(now.plusSeconds(accessTokenValiditySeconds)))
      .signWith(secretKey)
      .compact();
  }

  // access token 검증
  public Claims parse(String token) throws JwtException {
    return Jwts.parser()
      .verifyWith(secretKey)
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }

  // 아이디 추출
  public Long getUserId(Claims claims) {
    return Long.valueOf(claims.getSubject());
  }

  // 이메일 추출
  public String getEmail(Claims claims) {
    return claims.get(CLAIM_EMAIL, String.class);
  }

  // 토큰 버전 추출
  public int getTokenVersion(Claims claims) {
    return claims.get(CLAIM_TOKEN_VERSION, Integer.class);
  }
}
