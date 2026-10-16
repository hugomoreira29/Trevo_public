package pt.trevo.trevo_api.auth;

import pt.trevo.trevo_api.persistence.entity.User;

public interface PasswordResetEmailSender {

    void sendPasswordResetEmail(User user, String token);
}
