package com.semi.simlogistics.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.dto.*;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneDraftMapper;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneMapper;
import com.semi.simlogistics.web.infrastructure.persistence.systemconfig.mapper.SystemConfigMapper;
import com.semi.simlogistics.web.service.SceneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SceneController.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@WebMvcTest(controllers = SceneController.class)
class SceneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean(name = "sceneService")
    private SceneService sceneService;

    @MockBean
    private SceneMapper sceneMapper;

    @MockBean
    private SceneDraftMapper sceneDraftMapper;

    @MockBean
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private SceneDetailDTO testSceneDetail;
    private SceneSummaryDTO testSceneSummary;

    @BeforeEach
    void setUp() {
        testSceneDetail = createTestSceneDetail();
        testSceneSummary = createTestSceneSummary();
    }

    @Test
    void getScenes_shouldReturnPagedResult() throws Exception {
        // Given
        PagedResultDTO<SceneSummaryDTO> pagedResult = new PagedResultDTO<>(
                List.of(testSceneSummary),
                1,
                0,
                20
        );
        when(sceneService.getScenes(0, 20, null)).thenReturn(pagedResult);

        // When/Then
        mockMvc.perform(get("/api/v1/scenes")
                        .param("page", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void getScenes_shouldUseZeroBasedPageSemantics() throws Exception {
        // Given
        PagedResultDTO<SceneSummaryDTO> pagedResult = new PagedResultDTO<>(
                List.of(testSceneSummary),
                1,
                0,
                20
        );
        when(sceneService.getScenes(0, 20, "fab")).thenReturn(pagedResult);

        // When
        mockMvc.perform(get("/api/v1/scenes")
                        .param("page", "0")
                        .param("pageSize", "20")
                        .param("search", "fab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0));

        // Then
        verify(sceneService).getScenes(0, 20, "fab");
    }

    @Test
    void getScenes_pageZeroReturnsFirstPage() throws Exception {
        // Given - page=0 should return first page (0-based backend semantics)
        PagedResultDTO<SceneSummaryDTO> pagedResult = new PagedResultDTO<>(
                List.of(testSceneSummary),
                25,  // total items
                0,   // page=0 (first page, 0-based)
                10   // pageSize
        );
        when(sceneService.getScenes(0, 10, null)).thenReturn(pagedResult);

        // When/Then
        mockMvc.perform(get("/api/v1/scenes")
                        .param("page", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.total").value(25))
                .andExpect(jsonPath("$.data.totalPages").value(3));

        // Verify service received 0-based page number
        verify(sceneService).getScenes(0, 10, null);
    }

    @Test
    void getScenes_pageOneReturnsSecondPage() throws Exception {
        // Given - page=1 should return second page (0-based backend semantics)
        PagedResultDTO<SceneSummaryDTO> pagedResult = new PagedResultDTO<>(
                List.of(testSceneSummary),
                25,  // total items
                1,   // page=1 (second page, 0-based)
                10   // pageSize
        );
        when(sceneService.getScenes(1, 10, null)).thenReturn(pagedResult);

        // When/Then
        mockMvc.perform(get("/api/v1/scenes")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1));

        // Verify service received page=1 (0-based)
        verify(sceneService).getScenes(1, 10, null);
    }

    @Test
    void getSceneById_whenExists_shouldReturnScene() throws Exception {
        // Given
        when(sceneService.getSceneById("SCENE-001")).thenReturn(testSceneDetail);

        // When/Then
        mockMvc.perform(get("/api/v1/scenes/SCENE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sceneId").value("SCENE-001"))
                .andExpect(jsonPath("$.data.name").value("Test Scene"));
    }

    @Test
    void createScene_withValidData_shouldReturnCreatedScene() throws Exception {
        // Given
        CreateSceneRequestDTO request = new CreateSceneRequestDTO();
        request.setName("New Scene");
        request.setDescription("Test description");
        request.setEntities(new ArrayList<>());
        request.setPaths(new ArrayList<>());
        request.setProcessFlows(new ArrayList<>());

        when(sceneService.createScene(any(CreateSceneRequestDTO.class))).thenReturn(testSceneDetail);

        // When/Then
        mockMvc.perform(post("/api/v1/scenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sceneId").value("SCENE-001"));
    }

    @Test
    void deleteScene_withValidId_shouldReturnNoContent() throws Exception {
        // When/Then
        mockMvc.perform(delete("/api/v1/scenes/SCENE-001"))
                .andExpect(status().isNoContent());
    }

    // Helper methods

    private SceneDetailDTO createTestSceneDetail() {
        SceneDetailDTO dto = new SceneDetailDTO();
        dto.setSceneId("SCENE-001");
        dto.setName("Test Scene");
        dto.setDescription("Test description");
        dto.setVersion(1);
        dto.setEntities(new ArrayList<>());
        dto.setPaths(new ArrayList<>());
        dto.setProcessFlows(new ArrayList<>());
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private SceneSummaryDTO createTestSceneSummary() {
        SceneSummaryDTO dto = new SceneSummaryDTO();
        dto.setSceneId("SCENE-001");
        dto.setName("Test Scene");
        dto.setDescription("Test description");
        dto.setVersion(1);
        dto.setEntityCount(0);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
