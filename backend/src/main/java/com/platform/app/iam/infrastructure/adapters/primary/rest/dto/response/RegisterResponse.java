package com.platform.app.iam.infrastructure.adapters.primary.rest.dto.response;

import java.util.Set;
import java.util.UUID;

import com.platform.app.iam.application.dto.UserProfileDto;

import lombok.Builder;

@Builder
public record RegisterResponse(
    UUID id,
    String email,
    String fullName,
    Set<String> roles) {

  public static RegisterResponse from(UserProfileDto profile) {
    return RegisterResponse.builder()
        .id(profile.id())
        .email(profile.email())
        .fullName(profile.fullName())
        .roles(profile.roles())
        .build();
  }
}
