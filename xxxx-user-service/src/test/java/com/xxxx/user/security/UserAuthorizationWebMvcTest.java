package com.xxxx.user.security;

import com.xxxx.user.config.JwtSecurityConfig;
import com.xxxx.user.controller.EmployeeController;
import com.xxxx.user.controller.UserController;
import com.xxxx.user.controller.dto.response.EmployeeResponse;
import com.xxxx.user.controller.dto.response.UserResponse;
import com.xxxx.user.service.EmployeeService;
import com.xxxx.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {EmployeeController.class, UserController.class})
@Import(JwtSecurityConfig.class)
@TestPropertySource(properties = {
        "gateway.jwt.secret=test-jwt-key-must-be-at-least-32-bytes",
        "gateway.jwt.issuer=xxxx-user-service"
})
class UserAuthorizationWebMvcTest {

    private static final String TEST_JWT_KEY = "test-jwt-key-must-be-at-least-32-bytes";
    private static final String ISSUER = "xxxx-user-service";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private UserService userService;

    @Test
    void rejectsMissingJwtOnPrivateEndpoint() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void rejectsNormalUserCallingAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/employees")
                        .header("Authorization", bearer("1", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("Forbidden"));

        verify(employeeService, never()).getAllEmployees();
    }

    @Test
    void acceptsAdminCallingAdminEndpoint() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(new EmployeeResponse()));

        mockMvc.perform(get("/api/employees")
                        .header("Authorization", bearer("9", "ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void enforcesOwnerRuleForUserLookup() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(new UserResponse());

        mockMvc.perform(get("/api/users/1")
                        .header("Authorization", bearer("1", "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/users/2")
                        .header("Authorization", bearer("1", "USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"));
    }

    private String bearer(String subject, String roles) {
        return "Bearer " + token(subject, roles);
    }

    private String token(String subject, String roles) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_KEY.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuer(ISSUER)
                .id("mvc-token-id")
                .claim("email", "user@example.com")
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(key)
                .compact();
    }
}