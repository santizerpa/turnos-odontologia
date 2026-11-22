package com.turnosodontologia;

import com.turnosodontologia.dentist.DentistRepository;
import com.turnosodontologia.support.AuthFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthCookieAttributesTests {

    @Autowired
    DentistRepository dentists;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${local.server.port}")
    int port;

    @Test
    void loginCookieIsHttpOnlyAndSameSiteLax() throws Exception {
        String email = AuthFixtures.uniqueEmail("cookie");
        AuthFixtures.createActive(dentists, passwordEncoder, email, "demo-ficticia-123");

        HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager())
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/auth/login"))
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"email\":\"" + email + "\",\"password\":\"demo-ficticia-123\"}"))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().allValues("Set-Cookie"))
            .anyMatch(cookie -> cookie.contains("JSESSIONID=")
                && cookie.contains("HttpOnly")
                && cookie.matches("(?i).*samesite=lax.*"));
    }
}
