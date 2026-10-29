package com.example.customerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Default configuration: the demo page must not be reachable.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DemoUiDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoPageIsBlocked() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/index.html")).andExpect(status().isUnauthorized());
    }
}
