package com.semi.simlogistics.web.infrastructure.persistence.scene.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.dto.SceneSummaryDTO;
import com.semi.simlogistics.web.domain.scene.Scene;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MysqlSceneRepository.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@SpringBootTest
@ActiveProfiles("test")
class MysqlSceneRepositoryTest {

    @Autowired
    private SceneMapper sceneMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private MysqlSceneRepository sceneRepository;

    @BeforeEach
    void setUp() {
        sceneRepository = new MysqlSceneRepository(sceneMapper, objectMapper);

        // Clean up test data
        sceneMapper.delete(null);
    }

    @Test
    void save_shouldInsertNewScene() {
        // Given
        Scene scene = createTestScene("SCENE-001", "Test Scene");

        // When
        Scene savedScene = sceneRepository.save(scene);

        // Then
        assertThat(savedScene.getSceneId()).isEqualTo("SCENE-001");
        assertThat(savedScene.getName()).isEqualTo("Test Scene");
        assertThat(savedScene.getVersion()).isEqualTo(1);
        assertThat(savedScene.getCreatedAt()).isNotNull();
        assertThat(savedScene.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_whenExists_shouldReturnScene() {
        // Given
        Scene scene = createTestScene("SCENE-002", "Find Me");
        sceneRepository.save(scene);

        // When
        Optional<Scene> result = sceneRepository.findById("SCENE-002");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Find Me");
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        // When
        Optional<Scene> result = sceneRepository.findById("NONEXISTENT");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByName_whenExists_shouldReturnScene() {
        // Given
        Scene scene = createTestScene("SCENE-003", "Unique Name");
        sceneRepository.save(scene);

        // When
        Optional<Scene> result = sceneRepository.findByName("Unique Name");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getSceneId()).isEqualTo("SCENE-003");
    }

    @Test
    void findAll_shouldReturnPagedScenes() {
        // Given
        sceneRepository.save(createTestScene("SCENE-001", "Scene 1"));
        sceneRepository.save(createTestScene("SCENE-002", "Scene 2"));
        sceneRepository.save(createTestScene("SCENE-003", "Scene 3"));

        // When
        List<SceneSummaryDTO> result = sceneRepository.findAll(0, 2);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void count_shouldReturnTotalCount() {
        // Given
        sceneRepository.save(createTestScene("SCENE-001", "Scene 1"));
        sceneRepository.save(createTestScene("SCENE-002", "Scene 2"));

        // When
        long count = sceneRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void search_withKeyword_shouldReturnMatchingScenes() {
        // Given
        sceneRepository.save(createTestScene("SCENE-001", "Production Line A"));
        sceneRepository.save(createTestScene("SCENE-002", "Production Line B"));
        sceneRepository.save(createTestScene("SCENE-003", "Warehouse Scene"));

        // When
        List<SceneSummaryDTO> result = sceneRepository.search("Production", 0, 10);

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    void deleteById_shouldDeleteScene() {
        // Given
        Scene scene = createTestScene("SCENE-001", "To Delete");
        sceneRepository.save(scene);

        // When
        sceneRepository.deleteById("SCENE-001");

        // Then
        Optional<Scene> result = sceneRepository.findById("SCENE-001");
        assertThat(result).isEmpty();
    }

    @Test
    void existsByName_whenExists_shouldReturnTrue() {
        // Given
        Scene scene = createTestScene("SCENE-001", "Existing Name");
        sceneRepository.save(scene);

        // When
        boolean exists = sceneRepository.existsByName("Existing Name");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_whenNotExists_shouldReturnFalse() {
        // When
        boolean exists = sceneRepository.existsByName("Nonexistent Name");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void existsByNameExcluding_shouldExcludeProvidedId() {
        // Given
        Scene scene = createTestScene("SCENE-001", "Same Name");
        sceneRepository.save(scene);

        // When
        boolean exists = sceneRepository.existsByNameExcluding("Same Name", "SCENE-001");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void update_shouldIncrementVersion() {
        // Given
        Scene originalScene = sceneRepository.save(createTestScene("SCENE-001", "Original"));
        originalScene.setName("Updated");
        originalScene.setVersion(2);

        // When
        Scene updatedScene = sceneRepository.save(originalScene);

        // Then
        assertThat(updatedScene.getName()).isEqualTo("Updated");
        assertThat(updatedScene.getVersion()).isEqualTo(2);
    }

    @Test
    void save_whenUpdatingExistingScene_shouldNotInsertNewRow() {
        // Given
        Scene created = sceneRepository.save(createTestScene("SCENE-001", "Original"));
        long beforeUpdateCount = sceneMapper.selectCount(null);

        // Simulate a domain object that only carries business sceneId for update
        Scene update = new Scene();
        update.setSceneId("SCENE-001");
        update.setName("Updated Name");
        update.setDescription("Updated description");
        update.setVersion(created.getVersion() + 1);
        update.setEntities(new ArrayList<>());
        update.setPaths(new ArrayList<>());
        update.setProcessFlows(new ArrayList<>());
        update.setUpdatedAt(LocalDateTime.now());

        // When
        Scene saved = sceneRepository.save(update);
        long afterUpdateCount = sceneMapper.selectCount(null);

        // Then
        assertThat(saved.getSceneId()).isEqualTo("SCENE-001");
        assertThat(saved.getName()).isEqualTo("Updated Name");
        assertThat(afterUpdateCount).isEqualTo(beforeUpdateCount);
    }

    @Test
    void shouldKeepStableRowCountAcrossCreateCopyUpdateDelete() {
        // create
        sceneRepository.save(createTestScene("SCENE-001", "Scene A"));
        assertThat(sceneMapper.selectCount(null)).isEqualTo(1);

        // copy (new business id)
        sceneRepository.save(createTestScene("SCENE-002", "Scene A Copy"));
        assertThat(sceneMapper.selectCount(null)).isEqualTo(2);

        // update existing row
        Scene update = new Scene();
        update.setSceneId("SCENE-001");
        update.setName("Scene A Updated");
        update.setDescription("Updated");
        update.setVersion(2);
        update.setEntities(new ArrayList<>());
        update.setPaths(new ArrayList<>());
        update.setProcessFlows(new ArrayList<>());
        update.setUpdatedAt(LocalDateTime.now());
        sceneRepository.save(update);
        assertThat(sceneMapper.selectCount(null)).isEqualTo(2);

        // delete one row
        sceneRepository.deleteById("SCENE-002");
        assertThat(sceneMapper.selectCount(null)).isEqualTo(1);
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
        return scene;
    }
}
