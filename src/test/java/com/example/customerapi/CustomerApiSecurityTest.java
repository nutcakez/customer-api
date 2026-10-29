package com.example.customerapi;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "customer-api.seed-file=classpath:data/customers.json")
@AutoConfigureMockMvc
class CustomerApiSecurityTest {

  private static final String NEW_CUSTOMER =
      """
      {"firstName":"Test","lastName":"User","email":"test.user@example.com","status":"ACTIVE","department":"sales"}
      """;

  @Autowired private MockMvc mockMvc;

  private static RequestPostProcessor reader(String department) {
    return jwt()
        .jwt(jwt -> jwt.claim("department", department))
        .authorities(new SimpleGrantedAuthority("ROLE_customer-reader"));
  }

  private static RequestPostProcessor admin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_customer-admin"));
  }

  @Test
  void requestWithoutTokenIsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
  }

  @Test
  void tokenWithoutRoleIsForbidden() throws Exception {
    mockMvc.perform(get("/api/customers").with(jwt())).andExpect(status().isForbidden());
  }

  @Test
  void readerSeesOnlyOwnDepartment() throws Exception {
    mockMvc
        .perform(get("/api/customers").with(reader("sales")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].department", everyItem(equalTo("sales"))));
  }

  @Test
  void readerCanReadCustomerOfOwnDepartment() throws Exception {
    mockMvc.perform(get("/api/customers/1").with(reader("sales"))).andExpect(status().isOk());
  }

  @Test
  void readerCannotReadCustomerOfOtherDepartment() throws Exception {
    mockMvc
        .perform(get("/api/customers/4").with(reader("sales")))
        .andExpect(status().isForbidden());
  }

  @Test
  void readerCannotModify() throws Exception {
    mockMvc
        .perform(
            post("/api/customers")
                .with(reader("sales"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CUSTOMER))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/customers/1")
                .with(reader("sales"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CUSTOMER))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(delete("/api/customers/1").with(reader("sales")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminSeesAllDepartments() throws Exception {
    mockMvc.perform(get("/api/customers/7").with(admin())).andExpect(status().isOk());
  }

  @Test
  void adminCanCreateUpdateAndDelete() throws Exception {
    String location =
        mockMvc
            .perform(
                post("/api/customers")
                    .with(admin())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(NEW_CUSTOMER))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    mockMvc
        .perform(
            put(location)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CUSTOMER.replace("ACTIVE", "INACTIVE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("INACTIVE"));

    mockMvc.perform(delete(location).with(admin())).andExpect(status().isNoContent());
    mockMvc
        .perform(get(location).with(admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void createWithExistingEmailIsConflict() throws Exception {
    mockMvc
        .perform(
            post("/api/customers")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CUSTOMER.replace("test.user@example.com", "Anna.Schmidt@example.com")))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.detail")
                .value("A customer with email Anna.Schmidt@example.com already exists"));
  }

  @Test
  void updateToEmailOfAnotherCustomerIsConflict() throws Exception {
    mockMvc
        .perform(
            put("/api/customers/2")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CUSTOMER.replace("test.user@example.com", "anna.schmidt@example.com")))
        .andExpect(status().isConflict());
  }

  @Test
  void updateKeepingOwnEmailIsAllowed() throws Exception {
    mockMvc
        .perform(
            put("/api/customers/8")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_CUSTOMER.replace("test.user@example.com", "hannes.koch@example.com")))
        .andExpect(status().isOk());
  }

  @Test
  void invalidPayloadIsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/customers")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(contains("department", "email", "firstName", "lastName", "status")));
  }

  @Test
  void malformedJsonIsProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/customers")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{oops"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400));
  }
}
