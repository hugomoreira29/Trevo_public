package pt.trevo.trevo_api.service;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.dto.UpdateProfileRequest;
import pt.trevo.trevo_api.dto.UserProfileResponse;
import pt.trevo.trevo_api.persistence.entity.Shop;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ShopRepository shopRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.shopRepository = shopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Shop shop = shopRepository.findByOwnerId(userId).orElse(null);

        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhone(),
            user.getRole(),
            user.getStatus(),
            shop != null ? shop.getName() : null,
            shop != null ? shop.getStatus() : null
        );
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        if (userId == null || request == null) {
            throw badRequest("Invalid profile update request");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!isBlank(request.fullName())) {
            user.setFullName(request.fullName().trim());
        }

        if (request.phone() != null) {
            user.setPhone(normalizeOptional(request.phone()));
        }

        updateEmailIfNeeded(user, request.email());
        updatePasswordIfNeeded(user, request.currentPassword(), request.newPassword());

        return userRepository.save(user);
    }

    private void updateEmailIfNeeded(User user, String email) {
        if (isBlank(email)) {
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (normalizedEmail.equals(user.getEmail())) {
            return;
        }

        Optional<User> existingUser = userRepository.findByEmail(normalizedEmail);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setEmail(normalizedEmail);
    }

    private void updatePasswordIfNeeded(User user, String currentPassword, String newPassword) {
        boolean hasCurrentPassword = !isBlank(currentPassword);
        boolean hasNewPassword = !isBlank(newPassword);

        if (!hasCurrentPassword && !hasNewPassword) {
            return;
        }
        if (hasCurrentPassword != hasNewPassword) {
            throw badRequest("Current password and new password are required");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
