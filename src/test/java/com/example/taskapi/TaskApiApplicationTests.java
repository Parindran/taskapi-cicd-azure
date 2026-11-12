package com.example.taskapi;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
        // fails if the application context cannot start
    }

    @Test
    void listsSeededTasks() throws Exception {
        // other tests share this context and may add rows, so assert on content
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title == 'Set up the project')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.title == 'Ship it')]", hasSize(1)));
    }

    @Test
    void filtersByCompletedFlag() throws Exception {
        mockMvc.perform(get("/api/tasks").param("completed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.completed == false)]", hasSize(0)));
    }

    @Test
    void createsATask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Review the PR\",\"completed\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Review the PR"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void rejectsABlankTitle() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"completed\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsAppInfo() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("taskapi"))
                .andExpect(jsonPath("$.version").isString());
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void servesTheFrontend() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk());
    }
}
