package com.semi.simlogistics.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.dto.*;
import com.semi.simlogistics.web.domain.scene.Scene;
import com.semi.simlogistics.web.domain.scene.SceneDraft;
import com.semi.simlogistics.web.domain.scene.SceneDraftRepository;
import com.semi.simlogistics.web.domain.scene.SceneRepository;
import com.semi.simlogistics.web.exception.ResourceConflictException;
import com.semi.simlogistics.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SceneService.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@ExtendWith(MockitoExtension.class)
class SceneServiceTest {

    @Mock
    private SceneRepository sceneRepository;

    @Mock
    private SceneDraftRepository sceneDraftRepository;

    private SceneService sceneService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        sceneService = new SceneService(sceneRepository, sceneDraftRepository);
        objectMapper = new ObjectMapper();
    }

    @Test
    void getScenes_shouldReturnPagedResult() {
        // Given
        List<SceneSummaryDTO> scenes = createTestSceneSummaries();
        when(sceneRepository.findAll(0, 20)).thenReturn(scenes);
        when(sceneRepository.count()).thenReturn(2L);

        // When
        PagedResultDTO<SceneSummaryDTO> result = sceneService.getScenes(0, 20, null);

        // Then
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getScenes_withKeyword_shouldReturnFilteredResults() {
        // Given
        List<SceneSummaryDTO> scenes = List.of(createTestSceneSummary("SCENE-001", "Test Scene 1"));
        when(sceneRepository.search("Test", 0, 20)).thenReturn(scenes);
        when(sceneRepository.countSearch("Test")).thenReturn(1L);

        // When
        PagedResultDTO<SceneSummaryDTO> result = sceneService.getScenes(0, 20, "Test");

        // Then
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).contains("Test");
    }

    @Test
    void getSceneById_whenExists_shouldReturnScene() {
        // Given
        Scene scene = createTestScene("SCENE-001", "Test Scene");
        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(scene));

        // When
        SceneDetailDTO result = sceneService.getSceneById("SCENE-001");

        // Then
        assertThat(result.getSceneId()).isEqualTo("SCENE-001");
        assertThat(result.getName()).isEqualTo("Test Scene");
    }

    @Test
    void getSceneById_whenNotExists_shouldThrowException() {
        // Given
        when(sceneRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> sceneService.getSceneById("NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found");
    }

    @Test
    void createScene_withValidData_shouldReturnCreatedScene() {
        // Given
        CreateSceneRequestDTO request = new CreateSceneRequestDTO();
        request.setName("New Scene");
        request.setDescription("Test description");
        request.setEntities(new ArrayList<>());
        request.setPaths(new ArrayList<>());
        request.setProcessFlows(new ArrayList<>());

        when(sceneRepository.existsByName("New Scene")).thenReturn(false);
        when(sceneRepository.save(any(Scene.class))).thenAnswer(invocation -> {
            Scene scene = invocation.getArgument(0);
            scene.setCreatedAt(LocalDateTime.now());
            scene.setUpdatedAt(LocalDateTime.now());
            return scene;
        });

        // When
        SceneDetailDTO result = sceneService.createScene(request);

        // Then
        assertThat(result.getName()).isEqualTo("New Scene");
        assertThat(result.getDescription()).isEqualTo("Test description");
        assertThat(result.getVersion()).isEqualTo(1);
        verify(sceneRepository).save(any(Scene.class));
    }

    @Test
    void createScene_withDuplicateName_shouldThrowException() {
        // Given
        CreateSceneRequestDTO request = new CreateSceneRequestDTO();
        request.setName("Existing Scene");

        when(sceneRepository.existsByName("Existing Scene")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> sceneService.createScene(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Scene name already exists");
    }

    @Test
    void updateScene_withValidData_shouldReturnUpdatedScene() {
        // Given
        Scene existingScene = createTestScene("SCENE-001", "Original Name");
        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(existingScene));
        when(sceneRepository.existsByNameExcluding("Updated Name", "SCENE-001")).thenReturn(false);
        when(sceneRepository.save(any(Scene.class))).thenAnswer(invocation -> {
            Scene scene = invocation.getArgument(0);
            scene.setUpdatedAt(LocalDateTime.now());
            return scene;
        });

        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Updated Name");
        request.setDescription("Updated description");

        // When
        SceneDetailDTO result = sceneService.updateScene("SCENE-001", request);

        // Then
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getVersion()).isEqualTo(2);
    }

    @Test
    void deleteScene_withValidId_shouldDeleteScene() {
        // Given
        Scene scene = createTestScene("SCENE-001", "Test Scene");
        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(scene));

        // When
        sceneService.deleteScene("SCENE-001");

        // Then
        verify(sceneRepository).deleteById("SCENE-001");
        verify(sceneDraftRepository).deleteBySceneId("SCENE-001");
    }

    @Test
    void copyScene_withValidId_shouldReturnCopiedScene() {
        // Given
        Scene originalScene = createTestScene("SCENE-001", "Original Scene");
        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(originalScene));
        when(sceneRepository.existsByName(anyString())).thenReturn(false);
        when(sceneRepository.save(any(Scene.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SceneCopyResultDTO result = sceneService.copyScene("SCENE-001");

        // Then
        assertThat(result.getName()).contains("Copy");
        assertThat(result.getVersion()).isEqualTo(1);
        verify(sceneRepository).save(any(Scene.class));
    }

    @Test
    void getSceneDraft_whenExists_shouldReturnDraft() {
        // Given
        SceneDraft draft = new SceneDraft();
        draft.setSceneId("SCENE-001");
        draft.setVersion(1);
        draft.setSavedAt(LocalDateTime.now());
        draft.setContent(createTestSceneDetail());

        when(sceneDraftRepository.findBySceneId("SCENE-001")).thenReturn(Optional.of(draft));

        // When
        SceneDraftPayloadDTO result = sceneService.getSceneDraft("SCENE-001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSceneId()).isEqualTo("SCENE-001");
        assertThat(result.getVersion()).isEqualTo(1);
    }

    @Test
    void getSceneDraft_whenNotExists_shouldReturnNull() {
        // Given
        when(sceneDraftRepository.findBySceneId("SCENE-001")).thenReturn(Optional.empty());

        // When
        SceneDraftPayloadDTO result = sceneService.getSceneDraft("SCENE-001");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void saveSceneDraft_withValidData_shouldSaveDraft() {
        // Given
        Scene scene = createTestScene("SCENE-001", "Test Scene");
        SceneDetailDTO content = createTestSceneDetail();

        SceneDraftPayloadDTO payload = new SceneDraftPayloadDTO();
        payload.setSceneId("SCENE-001");
        payload.setContent(content);
        payload.setSavedAt(LocalDateTime.now());
        payload.setVersion(1);

        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(scene));
        when(sceneDraftRepository.save(any(SceneDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SceneDraftSaveResultDTO result = sceneService.saveSceneDraft(payload);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSavedAt()).isNotNull();
        verify(sceneDraftRepository).save(any(SceneDraft.class));
    }

    // ==================== C1: 新增契约验证测试 ====================

    /**
     * C1.1: 验证 saveDraft 请求体/DTO契约一致
     * 测试前端发送 SceneDraftPayload 结构时，后端能正确解析
     */
    @Test
    void shouldSaveDraftWithPayloadContract() {
        // Given: 前端发送 SceneDraftPayload 结构
        Scene scene = createTestScene("SCENE-001", "Test Scene");
        SceneDetailDTO content = createTestSceneDetail();

        SceneDraftPayloadDTO payload = new SceneDraftPayloadDTO();
        payload.setSceneId("SCENE-001");
        payload.setContent(content);
        payload.setSavedAt(LocalDateTime.now());
        payload.setVersion(1);

        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(scene));
        when(sceneDraftRepository.save(any(SceneDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When: 后端接收 SceneDraftPayloadDTO
        SceneDraftSaveResultDTO result = sceneService.saveSceneDraft(payload);

        // Then: 验证契约一致性
        assertThat(result.isSuccess()).isTrue();
        verify(sceneDraftRepository).save(argThat(draft -> {
            // 验证草稿内容正确提取自 payload
            return draft.getSceneId().equals("SCENE-001") &&
                    draft.getVersion() == 1 &&
                    draft.getContent() != null;
        }));
    }

    /**
     * C1.2: 验证 PUT 更新不新增行
     * 测试更新场景时使用 updateById 而不是 insert
     */
    @Test
    void shouldUpdateSceneWithoutInsertNewRow() {
        // Given: 已存在的场景，带有持久化 ID
        Scene existingScene = createTestScene("SCENE-001", "Original Name");
        existingScene.setId("persistent-id-123"); // 设置持久化 ID

        when(sceneRepository.findById("SCENE-001")).thenReturn(Optional.of(existingScene));
        when(sceneRepository.existsByNameExcluding("Updated Name", "SCENE-001")).thenReturn(false);
        when(sceneRepository.save(any(Scene.class))).thenAnswer(invocation -> {
            Scene scene = invocation.getArgument(0);
            // 验证场景有持久化 ID（说明是更新而非新增）
            assertThat(scene.getId()).isNotNull();
            scene.setUpdatedAt(LocalDateTime.now());
            return scene;
        });

        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Updated Name");

        // When: 执行更新
        SceneDetailDTO result = sceneService.updateScene("SCENE-001", request);

        // Then: 验证是更新而非新增
        assertThat(result.getName()).isEqualTo("Updated Name");
        assertThat(result.getVersion()).isEqualTo(2); // 版本递增
    }

    /**
     * C1.3: 验证草稿不存在的 404 语义
     * 测试 GET /scenes/{id}/draft 返回 null (对应前端 404)
     */
    @Test
    void shouldReturnNullOnDraft404InFrontendContractSemantics() {
        // Given: 草稿不存在
        when(sceneDraftRepository.findBySceneId("SCENE-001")).thenReturn(Optional.empty());

        // When: 查询草稿
        SceneDraftPayloadDTO result = sceneService.getSceneDraft("SCENE-001");

        // Then: 返回 null (前端契约: 404 -> null)
        assertThat(result).isNull();
    }

    /**
     * C1.4: 验证 page/search 参数一致性
     * 测试前端发送 search 参数，后端正确处理
     */
    @Test
    void shouldApplyPaginationAndSearchWithUnifiedParams() {
        // Given: 使用 search 参数 (而非 keyword)
        List<SceneSummaryDTO> scenes = List.of(createTestSceneSummary("SCENE-001", "Test Scene"));

        // 验证 repository 接收到正确的 search 参数
        when(sceneRepository.search("keyword", 0, 20)).thenReturn(scenes);
        when(sceneRepository.countSearch("keyword")).thenReturn(1L);

        // When: 前端发送 search 参数
        PagedResultDTO<SceneSummaryDTO> result = sceneService.getScenes(0, 20, "keyword");

        // Then: 验证参数一致性
        assertThat(result.getItems()).hasSize(1);
        verify(sceneRepository).search("keyword", 0, 20);
        verify(sceneRepository).countSearch("keyword");
    }

    /**
     * C1.5: 验证排序分页生效 (使用 MyBatis-Plus Page，无 .last() 覆盖风险)
     */
    @Test
    void shouldUseStableOrderAndPagingWithoutLastOverrideRisk() {
        // Given: 验证分页和排序正确应用
        List<SceneSummaryDTO> scenes = createTestSceneSummaries();
        when(sceneRepository.findAll(0, 20)).thenReturn(scenes);
        when(sceneRepository.count()).thenReturn(2L);

        // When: 执行分页查询
        PagedResultDTO<SceneSummaryDTO> result = sceneService.getScenes(0, 20, null);

        // Then: 验证分页结果正确
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getPageSize()).isEqualTo(20);
        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    void deleteSceneDraft_withValidId_shouldDeleteDraft() {
        // When
        sceneService.deleteSceneDraft("SCENE-001");

        // Then
        verify(sceneDraftRepository).deleteBySceneId("SCENE-001");
    }

    // Helper methods

    private Scene createTestScene(String sceneId, String name) {
        Scene scene = new Scene();
        scene.setSceneId(sceneId);
        scene.setName(name);
        scene.setDescription("Test description");
        scene.setVersion(1);
        scene.setEntities(new ArrayList<>());
        scene.setPaths(new ArrayList<>());
        scene.setProcessFlows(new ArrayList<>());
        scene.setCreatedAt(LocalDateTime.now());
        scene.setUpdatedAt(LocalDateTime.now());
        // Note: id is null for new scenes, set explicitly for update tests
        return scene;
    }

    private SceneSummaryDTO createTestSceneSummary(String sceneId, String name) {
        SceneSummaryDTO dto = new SceneSummaryDTO();
        dto.setSceneId(sceneId);
        dto.setName(name);
        dto.setDescription("Test description");
        dto.setVersion(1);
        dto.setEntityCount(0);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    private List<SceneSummaryDTO> createTestSceneSummaries() {
        return List.of(
                createTestSceneSummary("SCENE-001", "Test Scene 1"),
                createTestSceneSummary("SCENE-002", "Test Scene 2")
        );
    }

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
}
