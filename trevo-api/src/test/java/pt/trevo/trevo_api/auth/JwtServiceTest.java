package pt.trevo.trevo_api.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.User;

class JwtServiceTest extends BaseUnitTest {

    private static final String SECRET = "test-secret-key-with-at-least-thirty-two-bytes";

    @Test
    void generatedTokenShouldContainExpectedClaims() {
        Instant now = Instant.parse("2026-04-08T12:00:00Z");
        JwtService jwtService = new JwtService(
            SECRET,
            Duration.ofMinutes(15),
            Clock.fixed(now, ZoneOffset.UTC)
        );
        User user = User.builder()
            .id(42L)
            .role(Role.SHOP_OWNER)
            .build();

        String token = jwtService.generateToken(user);

        assertDoesNotThrow(() -> jwtService.validateToken(token));
        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals(Role.SHOP_OWNER, jwtService.extractRole(token));
        assertEquals(now.plus(Duration.ofMinutes(15)), jwtService.extractExpiration(token));
    }

    @Test
    void expiredTokenShouldThrow() {
        Instant issuedAt = Instant.parse("2026-04-08T12:00:00Z");
        JwtService tokenGenerator = new JwtService(
            SECRET,
            Duration.ofMinutes(5),
            Clock.fixed(issuedAt, ZoneOffset.UTC)
        );
        JwtService tokenValidator = new JwtService(
            SECRET,
            Duration.ofMinutes(5),
            Clock.fixed(issuedAt.plus(Duration.ofMinutes(6)), ZoneOffset.UTC)
        );

        String token = tokenGenerator.generateToken(User.builder()
            .id(42L)
            .role(Role.BUYER)
            .build());

        assertThrows(JwtException.class, () -> tokenValidator.validateToken(token));
    }

    @Test
    void tamperedTokenShouldThrow() {
        JwtService jwtService = new JwtService(
            SECRET,
            Duration.ofMinutes(15),
            Clock.fixed(Instant.parse("2026-04-08T12:00:00Z"), ZoneOffset.UTC)
        );
        String token = jwtService.generateToken(User.builder()
            .id(42L)
            .role(Role.BUYER)
            .build());
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThrows(JwtException.class, () -> jwtService.validateToken(tamperedToken));
    }
}
