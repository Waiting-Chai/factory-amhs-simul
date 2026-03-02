package com.semi.simlogistics.web.infrastructure.persistence.scene.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.dto.EntityDTO;
import com.semi.simlogistics.web.dto.PathDTO;
import com.semi.simlogistics.web.dto.ProcessFlowBindingDTO;
import com.semi.simlogistics.web.dto.SceneDetailDTO;
import com.semi.simlogistics.web.dto.SceneSummaryDTO;
import com.semi.simlogistics.web.domain.scene.Scene;
import com.semi.simlogistics.web.domain.scene.SceneRepository;
import com.semi.simlogistics.web.infrastructure.persistence.scene.entity.SceneDO;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MySQL-based implementation of SceneRepository.
 * Aligned with new schema: scenes table with definition/metadata JSON fields.
 *
 * @author shentw
 * @version 1.1
 * @since 2026-02-10
 */
public class MysqlSceneRepository implements SceneRepository {

    private static final Logger logger = LoggerFactory.getLogger(MysqlSceneRepository.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final SceneMapper sceneMapper;
    private final ObjectMapper objectMapper;

    public MysqlSceneRepository(SceneMapper sceneMapper, ObjectMapper objectMapper) {
        this.sceneMapper = sceneMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Scene> findById(String sceneId) {
        SceneDO sceneDO = sceneMapper.selectById(sceneId);
        return Optional.ofNullable(toDomain(sceneDO));
    }

    @Override
    public Optional<Scene> findByName(String name) {
        SceneDO sceneDO = sceneMapper.selectOne(
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDO::getName, name)
        );
        return Optional.ofNullable(toDomain(sceneDO));
    }

    @Override
    public List<SceneSummaryDTO> findAll(int page, int pageSize) {
        // Use MyBatis-Plus Page for stable pagination
        IPage<SceneDO> pageResult = sceneMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
                        .orderByDesc(SceneDO::getCreatedAt)
        );
        return pageResult.getRecords().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return sceneMapper.selectCount(
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
        );
    }

    @Override
    public List<SceneSummaryDTO> search(String keyword, int page, int pageSize) {
        // Use MyBatis-Plus Page for stable pagination with search
        IPage<SceneDO> pageResult = sceneMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
                        .and(w -> w.like(SceneDO::getName, keyword)
                                .or()
                                .like(SceneDO::getDescription, keyword))
                        .orderByDesc(SceneDO::getCreatedAt)
        );
        return pageResult.getRecords().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long countSearch(String keyword) {
        return sceneMapper.selectCount(
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
                        .and(w -> w.like(SceneDO::getName, keyword)
                                .or()
                                .like(SceneDO::getDescription, keyword))
        );
    }

    @Override
    public Scene save(Scene scene) {
        SceneDO sceneDO = toDataObject(scene);
        LocalDateTime now = LocalDateTime.now();
        sceneDO.setUpdatedAt(now);
        sceneDO.setTenantId(DEFAULT_TENANT_ID);
        if (sceneDO.getSchemaVersion() == null || sceneDO.getSchemaVersion() <= 0) {
            sceneDO.setSchemaVersion(1);
        }

        String businessId = scene.getSceneId() != null ? scene.getSceneId() : scene.getId();
        SceneDO existing = businessId == null ? null : sceneMapper.selectById(businessId);

        if (existing == null) {
            if (sceneDO.getId() == null || sceneDO.getId().isBlank()) {
                sceneDO.setId(businessId != null ? businessId : UUID.randomUUID().toString());
            }
            if (sceneDO.getCreatedAt() == null) {
                sceneDO.setCreatedAt(now);
            }
            if (sceneDO.getVersion() == null || sceneDO.getVersion() <= 0) {
                sceneDO.setVersion(1);
            }
            sceneMapper.insert(sceneDO);
        } else {
            sceneDO.setId(existing.getId());
            sceneDO.setCreatedAt(existing.getCreatedAt());
            sceneDO.setTenantId(existing.getTenantId());
            sceneDO.setSchemaVersion(existing.getSchemaVersion());
            if (sceneDO.getVersion() == null || sceneDO.getVersion() <= 0) {
                sceneDO.setVersion(existing.getVersion());
            }
            sceneMapper.updateById(sceneDO);
        }

        SceneDO persisted = sceneMapper.selectById(sceneDO.getId());
        return toDomain(persisted != null ? persisted : sceneDO);
    }

    @Override
    public void deleteById(String sceneId) {
        sceneMapper.deleteById(sceneId);
    }

    @Override
    public boolean existsByName(String name) {
        return sceneMapper.selectCount(
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDO::getName, name)
        ) > 0;
    }

    @Override
    public boolean existsByNameExcluding(String name, String excludeSceneId) {
        return sceneMapper.selectCount(
                new LambdaQueryWrapper<SceneDO>()
                        .eq(SceneDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDO::getName, name)
                        .ne(SceneDO::getId, excludeSceneId)
        ) > 0;
    }

    /**
     * Convert SceneDO to Scene domain model.
     * Handles new schema with definition/metadata JSON fields.
     */
    private Scene toDomain(SceneDO sceneDO) {
        if (sceneDO == null) {
            return null;
        }

        Scene scene = new Scene();
        // Set persistent ID for update detection
        scene.setId(sceneDO.getId());
        scene.setSceneId(sceneDO.getId());
        scene.setName(sceneDO.getName());
        scene.setDescription(sceneDO.getDescription());
        scene.setVersion(sceneDO.getVersion());
        scene.setCreatedAt(sceneDO.getCreatedAt());
        scene.setUpdatedAt(sceneDO.getUpdatedAt());

        try {
            // Parse definition JSON to extract entities, paths, processFlows
            if (sceneDO.getDefinition() != null) {
                JsonNode definitionNode = objectMapper.readTree(sceneDO.getDefinition());

                if (definitionNode.has("entities")) {
                    List<EntityDTO> entities = objectMapper.readValue(
                            definitionNode.get("entities").toString(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, EntityDTO.class)
                    );
                    scene.setEntities(entities);
                }

                if (definitionNode.has("paths")) {
                    List<PathDTO> paths = objectMapper.readValue(
                            definitionNode.get("paths").toString(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, PathDTO.class)
                    );
                    scene.setPaths(paths);
                }

                if (definitionNode.has("processFlows")) {
                    List<ProcessFlowBindingDTO> processFlows = objectMapper.readValue(
                            definitionNode.get("processFlows").toString(),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, ProcessFlowBindingDTO.class)
                    );
                    scene.setProcessFlows(processFlows);
                }
            } else {
                // Fallback to empty collections for new schema
                scene.setEntities(new ArrayList<>());
                scene.setPaths(new ArrayList<>());
                scene.setProcessFlows(new ArrayList<>());
            }

            // Parse metadata JSON for entityCount
            if (sceneDO.getMetadata() != null) {
                JsonNode metadataNode = objectMapper.readTree(sceneDO.getMetadata());
                if (metadataNode.has("entityCount")) {
                    scene.setEntityCount(metadataNode.get("entityCount").asInt());
                }
            }

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse scene JSON data: {}", e.getMessage(), e);
            scene.setEntities(new ArrayList<>());
            scene.setPaths(new ArrayList<>());
            scene.setProcessFlows(new ArrayList<>());
        }

        return scene;
    }

    /**
     * Convert Scene domain model to SceneDO.
     * Handles new schema with definition/metadata JSON fields.
     */
    private SceneDO toDataObject(Scene scene) {
        SceneDO sceneDO = new SceneDO();
        String persistenceId = scene.getSceneId() != null ? scene.getSceneId() : scene.getId();
        sceneDO.setId(persistenceId);
        sceneDO.setSceneId(persistenceId);
        sceneDO.setName(scene.getName());
        sceneDO.setDescription(scene.getDescription());
        sceneDO.setVersion(scene.getVersion());
        sceneDO.setCreatedAt(scene.getCreatedAt());
        sceneDO.setUpdatedAt(scene.getUpdatedAt());

        try {
            // Build definition JSON from entities, paths, processFlows
            com.fasterxml.jackson.databind.node.ObjectNode definitionNode = objectMapper.createObjectNode();
            definitionNode.set("entities", objectMapper.valueToTree(
                    scene.getEntities() != null ? scene.getEntities() : new ArrayList<>()));
            definitionNode.set("paths", objectMapper.valueToTree(
                    scene.getPaths() != null ? scene.getPaths() : new ArrayList<>()));
            definitionNode.set("processFlows", objectMapper.valueToTree(
                    scene.getProcessFlows() != null ? scene.getProcessFlows() : new ArrayList<>()));

            sceneDO.setDefinition(objectMapper.writeValueAsString(definitionNode));

            // Build metadata JSON
            com.fasterxml.jackson.databind.node.ObjectNode metadataNode = objectMapper.createObjectNode();
            metadataNode.put("entityCount", scene.getEntityCount());
            sceneDO.setMetadata(objectMapper.writeValueAsString(metadataNode));

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize scene data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize scene data", e);
        }

        return sceneDO;
    }

    private SceneSummaryDTO toSummaryDTO(SceneDO sceneDO) {
        SceneSummaryDTO dto = new SceneSummaryDTO();
        dto.setSceneId(sceneDO.getId());
        dto.setName(sceneDO.getName());
        dto.setDescription(sceneDO.getDescription());
        dto.setVersion(sceneDO.getVersion());
        dto.setCreatedAt(sceneDO.getCreatedAt());
        dto.setUpdatedAt(sceneDO.getUpdatedAt());

        // Extract entityCount from metadata
        try {
            if (sceneDO.getMetadata() != null) {
                JsonNode metadataNode = objectMapper.readTree(sceneDO.getMetadata());
                dto.setEntityCount(metadataNode.has("entityCount") ?
                        metadataNode.get("entityCount").asInt() : 0);
            } else {
                dto.setEntityCount(0);
            }
        } catch (JsonProcessingException e) {
            logger.warn("Failed to parse metadata for scene {}: {}", sceneDO.getId(), e.getMessage());
            dto.setEntityCount(0);
        }

        return dto;
    }
}
