package com.speed_anwer.expensetracker.integration;

import com.speed_anwer.expensetracker.dto.request.LoginRequest;
import com.speed_anwer.expensetracker.dto.request.RegisterRequest;
import com.speed_anwer.expensetracker.dto.response.AuthResponse;
import com.speed_anwer.expensetracker.dto.response.UserResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    private final String email = "integration@test.com";
    private final String password = "password123";

    @Test
    @Order(1)
    void register_returnsTokensAndUser() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Integration User");
        request.setEmail(email);
        request.setPassword(password);

        AuthResponse body = restTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        assertThat(body.getAccessToken()).isNotBlank();
        assertThat(body.getRefreshToken()).isNotBlank();
        assertThat(body.getUser().getEmail()).isEqualTo(email);
    }

    @Test
    @Order(2)
    void protectedEndpoint_withoutToken_isUnauthorized() {
        restTestClient.get()
                .uri("/api/expenses")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(3)
    void login_withWrongPassword_isUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("wrong-password");

        restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(4)
    void meEndpoint_withValidToken_returnsCurrentUser() {
        String token = loginAndGetAccessToken();

        UserResponse me = restTestClient.get()
                .uri("/api/users/me")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserResponse.class)
                .returnResult().getResponseBody();

        assertThat(me).isNotNull();
        assertThat(me.getEmail()).isEqualTo(email);
    }

    @Test
    @Order(5)
    void logout_revokesAccessTokenImmediately() {
        String token = loginAndGetAccessToken();

        restTestClient.post()
                .uri("/api/auth/logout")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get()
                .uri("/api/expenses")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String loginAndGetAccessToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        AuthResponse body = restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .returnResult().getResponseBody();

        assertThat(body).isNotNull();
        return body.getAccessToken();
    }
}
