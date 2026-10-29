package com.example.customerapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "customer-api.demo-ui.enabled=true")
@AutoConfigureMockMvc
class DemoUiEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoPageIsPublic() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    void apiStillRequiresToken() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
    }
}
