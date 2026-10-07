package com.outforpavan.orderflow.learning;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningController.class)
@Import(LearningService.class)
class LearningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesTheLearningStatusAsJson() throws Exception {
        mockMvc.perform(get("/api/learning/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.application").value("orderflow"))
                .andExpect(jsonPath("$.message").value("LearningService - Learning Spring Boot one step at a time"));
    }
}
