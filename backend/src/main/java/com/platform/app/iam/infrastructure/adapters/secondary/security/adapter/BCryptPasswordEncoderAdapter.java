package com.platform.app.iam.infrastructure.adapters.secondary.security.adapter;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.platform.app.iam.application.ports.outbound.PasswordEncoderPort;

@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

  private final PasswordEncoder delegate;

  public BCryptPasswordEncoderAdapter() {
    // Rule B1: BCrypt with work factor >= 12
    this.delegate = new BCryptPasswordEncoder(12);
  }

  public BCryptPasswordEncoderAdapter(int strength) {
    this.delegate = new BCryptPasswordEncoder(Math.max(12, strength));
  }

  @Override
  public boolean matches(String rawPassword, String encodedPassword) {
    if (rawPassword == null || encodedPassword == null) {
      return false;
    }
    return delegate.matches(rawPassword, encodedPassword);
  }

  @Override
  public String encode(String rawPassword) {
    return delegate.encode(rawPassword);
  }
}
