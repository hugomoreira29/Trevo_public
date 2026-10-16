package pt.trevo.trevo_api.service;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pt.trevo.trevo_api.auth.LoginRequest;
import pt.trevo.trevo_api.auth.RegisterBuyerRequest;
import pt.trevo.trevo_api.auth.RegisterShopOwnerRequest;
import pt.trevo.trevo_api.auth.JwtService;
import pt.trevo.trevo_api.auth.TokenPairResponse;
import pt.trevo.trevo_api.persistence.entity.Role;
import pt.trevo.trevo_api.persistence.entity.User;
import pt.trevo.trevo_api.persistence.repository.RefreshTokenRepository;
import pt.trevo.trevo_api.persistence.repository.ShopRepository;
import pt.trevo.trevo_api.persistence.repository.UserRepository;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:user-controller;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@AutoConfigureMockMvc
class UserMeEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        shopRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void meEndpointReturnsBuyerProfileWithoutShopFields() throws Exception {
        RegisterBuyerRequest registerRequest = new RegisterBuyerRequest(
            "buyer.profile@example.com",
            "Secret123!",
            "Buyer Profile",
            "912345678"
        );

        mockMvc.perform(post("/api/auth/register/buyer")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        TokenPairResponse tokenPair = login(registerRequest.email(), registerRequest.password());

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(registerRequest.email()))
            .andExpect(jsonPath("$.role").value("BUYER"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.shopName").doesNotExist())
            .andExpect(jsonPath("$.shopStatus").doesNotExist());
    }

    @Test
    void meEndpointReturnsPendingShopOwnerAndShopStatus() throws Exception {
        RegisterShopOwnerRequest registerRequest = new RegisterShopOwnerRequest(
            "owner.profile@example.com",
            "Secret123!",
            "Owner Profile",
            "912345678",
            "Trevo Atelier",
            "123456789"
        );

        mockMvc.perform(post("/api/auth/register/shop-owner")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        TokenPairResponse tokenPair = login(registerRequest.email(), registerRequest.password());

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPair.accessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(registerRequest.email()))
            .andExpect(jsonPath("$.role").value("SHOP_OWNER"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.shopName").value(registerRequest.shopName()))
            .andExpect(jsonPath("$.shopStatus").value("PENDING"));
    }

    @Test
    void meEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meEndpointReturnsNotFoundWhenAuthenticatedUserRecordIsMissing() throws Exception {
        String token = jwtService.generateToken(User.builder()
            .id(999L)
            .role(Role.BUYER)
            .build());

        mockMvc.perform(get("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    private TokenPairResponse login(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
            .andExpect(status().isOk())
            .andReturn();

        return objectMapper.readValue(loginResult.getResponse().getContentAsByteArray(), TokenPairResponse.class);
    }
}
