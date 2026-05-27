package com.ssafy.tax7i.global.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-bytes-long!!";
    private static final long ACCESS_EXPIRATION = 1800000L;
    private static final long REFRESH_EXPIRATION = 604800000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        jwtTokenProvider = new JwtTokenProvider(props);
        jwtTokenProvider.init();
    }

    @Test
    void createAccessToken_유효한토큰생성() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
    }

    @Test
    void createRefreshToken_유효한토큰생성() {
        String token = jwtTokenProvider.createRefreshToken(42L);

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void validateToken_만료된토큰_false반환() throws InterruptedException {
        JwtProperties shortProps = new JwtProperties(SECRET, 1L, 1L);
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);
        shortProvider.init();

        String token = shortProvider.createAccessToken(1L);
        Thread.sleep(10);

        assertThat(shortProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_변조된토큰_false반환() {
        String token = jwtTokenProvider.createAccessToken(1L) + "tampered";

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_빈문자열_false반환() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    void getUserId_올바른userId반환() {
        String token = jwtTokenProvider.createAccessToken(42L);

        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    void getRemainingExpiration_양수반환() {
        String token = jwtTokenProvider.createAccessToken(1L);

        assertThat(jwtTokenProvider.getRemainingExpiration(token)).isPositive();
    }
}
