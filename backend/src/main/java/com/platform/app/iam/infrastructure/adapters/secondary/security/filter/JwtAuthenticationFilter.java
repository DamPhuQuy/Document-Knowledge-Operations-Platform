package com.platform.app.iam.infrastructure.adapters.secondary.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.platform.app.iam.infrastructure.adapters.secondary.security.adapter.JwtTokenProviderAdapter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProviderAdapter jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    String jwt = extractJwt(request);

    if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
      Claims claims = jwtTokenProvider.parseClaims(jwt);
      String userId = claims.getSubject();

      List<SimpleGrantedAuthority> authorities = new ArrayList<>();

      @SuppressWarnings("unchecked")
      List<String> permissions = claims.get("permissions", List.class);
      if (permissions != null) {
        for (String perm : permissions) {
          authorities.add(new SimpleGrantedAuthority(perm));
          authorities.add(new SimpleGrantedAuthority(perm.toLowerCase()));
        }
      }

      @SuppressWarnings("unchecked")
      List<String> roles = claims.get("roles", List.class);
      if (roles != null) {
        for (String role : roles) {
          if (role.startsWith("ROLE_")) {
            authorities.add(new SimpleGrantedAuthority(role));
            authorities.add(new SimpleGrantedAuthority(role.substring(5)));
          } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            authorities.add(new SimpleGrantedAuthority(role));
          }
        }
      }

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(userId, null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private String extractJwt(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
