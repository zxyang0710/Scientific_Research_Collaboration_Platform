package com.campus.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResearchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "zhangwei", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(response).path("data").path("token").asText();
    }

    @Test
    void dashboardAndProjectsReturnInitializedData() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectCount").value(2))
                .andExpect(jsonPath("$.data.recentProjects").isArray());

        mockMvc.perform(get("/api/projects").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].leaderName").isNotEmpty());
    }

    @Test
    void taskCanBeCreatedAndStatusUpdated() throws Exception {
        String createResponse = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "projectId", 1,
                                "title", "集成测试任务",
                                "description", "验证任务创建与进度更新",
                                "assigneeId", 2,
                                "priority", "HIGH",
                                "status", "TODO",
                                "deadline", "2026-12-31"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("集成测试任务"))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(createResponse).path("data");
        long taskId = data.path("id").asLong();

        mockMvc.perform(put("/api/tasks/{id}/status", taskId)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    @Test
    void projectDetailContainsCollaborationModules() throws Exception {
        mockMvc.perform(get("/api/projects/1").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project.name").value("面向复杂场景的多模态智能感知研究"))
                .andExpect(jsonPath("$.data.members").isArray())
                .andExpect(jsonPath("$.data.tasks").isArray())
                .andExpect(jsonPath("$.data.papers").isArray());
    }

    @Test
    void invalidLoginIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "zhangwei", "password", "wrong"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminCanListFilesAcrossAllProjects() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = objectMapper.readTree(response).path("data").path("token").asText();

        mockMvc.perform(get("/api/files?page=1&size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").value(4));
    }

    private String bearer() {
        return "Bearer " + token;
    }
}
