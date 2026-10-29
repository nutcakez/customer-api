package com.example.customerapi;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sends a real Bearer header through the full filter chain. Only the JwtDecoder (signature check
 * against Keycloak) is mocked, so this verifies that SecurityConfig actually wires in
 * KeycloakRealmRoleConverter - unlike the jwt() post-processor, which skips the bearer token filter.
 */
@SpringBootTest(properties = "customer-api.seed-file=classpath:data/customers.json")
@AutoConfigureMockMvc
class JwtFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void keycloakRolesAreMappedThroughTheRealFilterChain() throws Exception {
        Jwt token = Jwt.withTokenValue("token").header("alg", "none")
                .subject("sally")
                .claim("realm_access", Map.of("roles", List.of("customer-reader")))
                .claim("department", "sales")
                .build();
        when(jwtDecoder.decode("token")).thenReturn(token);

        mockMvc.perform(get("/api/customers/1").header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/customers/4").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/customers/1").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
    }
}
