package pt.trevo.trevo_api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.trevo.trevo_api.persistence.entity.User;

@Component
public class NoOpPasswordResetEmailSender implements PasswordResetEmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpPasswordResetEmailSender.class);

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        LOGGER.info("Password reset requested for {}", user.getEmail());
    }
}
