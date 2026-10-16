package pt.trevo.trevo_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.BaseUnitTest;
import pt.trevo.trevo_api.dto.UpdateProfileRequest;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

class UserServiceTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void updateProfileUpdatesNameAndPhoneWhenProvided() {
        User existingUser = User.builder()
            .id(10L)
            .email("buyer@example.com")
            .passwordHash("stored-hash")
            .fullName("Buyer Name")
            .phone("912345678")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build();
        UpdateProfileRequest request = new UpdateProfileRequest(" Updated Buyer ", null, " 934567890 ", null, null);

        when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateProfile(10L, request);

        assertThat(updatedUser.getFullName()).isEqualTo("Updated Buyer");
        assertThat(updatedUser.getPhone()).isEqualTo("934567890");
        assertThat(updatedUser.getEmail()).isEqualTo("buyer@example.com");
        assertThat(updatedUser.getPasswordHash()).isEqualTo("stored-hash");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateProfileRejectsEmailChangeWhenEmailAlreadyExists() {
        User existingUser = User.builder()
            .id(10L)
            .email("buyer@example.com")
            .passwordHash("stored-hash")
            .fullName("Buyer Name")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, "taken@example.com", null, null, null);

        when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("taken@example.com"))
            .thenReturn(Optional.of(User.builder().id(99L).email("taken@example.com").build()));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userService.updateProfile(10L, request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getReason()).isEqualTo("Email already exists");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfileChangesPasswordWhenOldPasswordMatches() {
        User existingUser = User.builder()
            .id(10L)
            .email("buyer@example.com")
            .passwordHash("stored-hash")
            .fullName("Buyer Name")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, "current-password", "new-password");

        when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("current-password", "stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedUser = userService.updateProfile(10L, request);

        assertThat(updatedUser.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(passwordEncoder).matches("current-password", "stored-hash");
        verify(passwordEncoder).encode("new-password");
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateProfileRejectsPasswordChangeWhenOldPasswordIsWrong() {
        User existingUser = User.builder()
            .id(10L)
            .email("buyer@example.com")
            .passwordHash("stored-hash")
            .fullName("Buyer Name")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build();
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, "wrong-password", "new-password");

        when(userRepository.findById(10L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userService.updateProfile(10L, request)
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.getReason()).isEqualTo("Current password is incorrect");
        verify(passwordEncoder).matches("wrong-password", "stored-hash");
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }
}
