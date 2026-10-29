package com.example.customerapi.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * The parts of the authenticated JWT that matter for claim-based authorization.
 */
public record CurrentUser(boolean isAdmin, String department) {

    public static final String DEPARTMENT_CLAIM = "department";

    public static CurrentUser from(JwtAuthenticationToken authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> ("ROLE_" + Roles.CUSTOMER_ADMIN).equals(authority.getAuthority()));
        return new CurrentUser(isAdmin, authentication.getToken().getClaimAsString(DEPARTMENT_CLAIM));
    }

    public boolean canAccessDepartment(String customerDepartment) {
        return isAdmin || (department != null && department.equalsIgnoreCase(customerDepartment));
    }
}
