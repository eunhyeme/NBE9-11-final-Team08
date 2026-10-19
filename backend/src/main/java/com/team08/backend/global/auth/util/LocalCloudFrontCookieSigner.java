package com.team08.backend.global.auth.util;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 로컬/테스트용 CloudFront 서명 쿠키 stub.
 * 로컬 영상은 /videos-local/** 로 인증 없이 서빙되므로 쿠키 값은 의미가 없다.
 */
@Component
@Profile("!prod")
public class LocalCloudFrontCookieSigner implements CloudFrontCookieSigner {

    @Override
    public ResponseCookie[] createSignedCookies(String resourcePath, String cookiePath) {
        return new ResponseCookie[]{
                buildCookie("CloudFront-Policy", "dummy-policy", cookiePath),
                buildCookie("CloudFront-Signature", "dummy-signature", cookiePath),
                buildCookie("CloudFront-Key-Pair-Id", "dummy-id", cookiePath)
        };
    }

    private ResponseCookie buildCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path(path)
                .maxAge(Duration.ofHours(1))
                .build();
    }
}
