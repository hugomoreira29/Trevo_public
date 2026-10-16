package pt.trevo.trevo_api.auth;

import java.time.Duration;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Builder
@ConfigurationProperties(prefix = "app.security.auth")
public record AuthProperties(Duration refreshTokenExpiration, Duration passwordResetTokenExpiration, Duration passwordResetCooldown) {

    public AuthProperties {
        if (refreshTokenExpiration == null) {
            refreshTokenExpiration = Duration.ofDays(30);
        }
        if (passwordResetTokenExpiration == null) {
            passwordResetTokenExpiration = Duration.ofMinutes(15);
        }
        if (passwordResetCooldown == null) {
            passwordResetCooldown = Duration.ofSeconds(60);
        }
    }
}
