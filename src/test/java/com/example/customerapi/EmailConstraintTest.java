package com.example.customerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.customerapi.customer.repository.CustomerRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simulates two concurrent creates with the same email: the service's exists-check is made to miss
 * the duplicate, so only the database's unique constraint catches it.
 */
@SpringBootTest(properties = "customer-api.seed-file=classpath:data/customers.json")
@AutoConfigureMockMvc
class EmailConstraintTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private CustomerRepository repository;

    @Test
    void uniqueConstraintViolationOnEmailIsConflict() throws Exception {
        doReturn(false).when(repository).existsByEmailIgnoreCase(anyString());

        mockMvc.perform(post("/api/customers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer-admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Anna","lastName":"Again","email":"anna.schmidt@example.com",\
                                "status":"ACTIVE","department":"sales"}
                                """))
                .andExpect(status().isConflict());
    }
}
