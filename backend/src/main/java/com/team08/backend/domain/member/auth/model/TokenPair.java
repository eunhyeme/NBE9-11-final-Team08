package com.team08.backend.domain.member.auth.model;

public record TokenPair(
        String accessToken,
        String refreshToken
) {
}
