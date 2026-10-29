package com.example.customerapi;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.customerapi.security.KeycloakRealmRoleConverter;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapsRealmRolesToPrefixedAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("customer-reader", "offline_access")))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_customer-reader", "ROLE_offline_access");
    }

    @Test
    void returnsNoAuthoritiesWithoutRealmAccessClaim() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
