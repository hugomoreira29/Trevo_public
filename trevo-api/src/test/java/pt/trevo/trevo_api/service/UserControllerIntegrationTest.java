package pt.trevo.trevo_api.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import pt.trevo.trevo_api.auth.AppUserDetailsService;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.config.SecurityConfig;
import pt.trevo.trevo_api.controller.UserController;
import pt.trevo.trevo_api.dto.UpdateProfileRequest;
import pt.trevo.trevo_api.dto.UserProfileResponse;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.Status;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;
import pt.trevo.trevo_api.security.SecurityUtils;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityUtils.class})
class UserControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ShopRepository shopRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void updateProfileExposesUserServiceThroughHttp() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Buyer", "buyer@example.com", "934567890", null, null);
        User updatedUser = User.builder()
            .id(10L)
            .email("buyer@example.com")
            .fullName("Updated Buyer")
            .phone("934567890")
            .role(Role.BUYER)
            .status(Status.ACTIVE)
            .build();
        UserProfileResponse profileResponse = new UserProfileResponse(
            10L, "buyer@example.com", "Updated Buyer", "934567890", Role.BUYER, Status.ACTIVE, null, null
        );

        when(userService.updateProfile(eq(10L), any(UpdateProfileRequest.class))).thenReturn(updatedUser);
        when(userService.getCurrentUserProfile(10L)).thenReturn(profileResponse);

        mockMvc.perform(
            put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.email").value("buyer@example.com"))
            .andExpect(jsonPath("$.fullName").value("Updated Buyer"))
            .andExpect(jsonPath("$.phone").value("934567890"))
            .andExpect(jsonPath("$.role").value("BUYER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "10", roles = "BUYER")
    void updateProfilePropagatesServiceErrors() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(null, "taken@example.com", null, null, null);

        when(userService.updateProfile(eq(10L), any(UpdateProfileRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"));

        mockMvc.perform(
            put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict());
    }
}
