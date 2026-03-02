package com.semi.simlogistics.web.infrastructure.persistence.scene.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.dto.SceneDetailDTO;
import com.semi.simlogistics.web.domain.scene.SceneDraft;
import com.semi.simlogistics.web.domain.scene.SceneDraftRepository;
import com.semi.simlogistics.web.infrastructure.persistence.scene.entity.SceneDraftDO;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneDraftMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL-based implementation of SceneDraftRepository.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class MysqlSceneDraftRepository implements SceneDraftRepository {

    private static final Logger logger = LoggerFactory.getLogger(MysqlSceneDraftRepository.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final SceneDraftMapper sceneDraftMapper;
    private final ObjectMapper objectMapper;

    public MysqlSceneDraftRepository(SceneDraftMapper sceneDraftMapper, ObjectMapper objectMapper) {
        this.sceneDraftMapper = sceneDraftMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SceneDraft> findBySceneId(String sceneId) {
        SceneDraftDO draftDO = sceneDraftMapper.selectOne(
                new LambdaQueryWrapper<SceneDraftDO>()
                        .eq(SceneDraftDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDraftDO::getSceneId, sceneId)
        );
        return Optional.ofNullable(toDomain(draftDO));
    }

    @Override
    public SceneDraft save(SceneDraft draft) {
        SceneDraftDO draftDO = toDataObject(draft);
        draftDO.setTenantId(DEFAULT_TENANT_ID);
        if (draftDO.getId() == null || draftDO.getId().isBlank()) {
            draftDO.setId(UUID.randomUUID().toString());
        }

        // Set savedAt only if not already set (preserve caller's value)
        if (draftDO.getSavedAt() == null) {
            draftDO.setSavedAt(LocalDateTime.now());
        }

        SceneDraftDO existing = sceneDraftMapper.selectOne(
                new LambdaQueryWrapper<SceneDraftDO>()
                        .eq(SceneDraftDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDraftDO::getSceneId, draft.getSceneId())
        );

        if (existing != null) {
            draftDO.setId(existing.getId());
            draftDO.setCreatedAt(existing.getCreatedAt());
            sceneDraftMapper.updateById(draftDO);
        } else {
            if (draftDO.getCreatedAt() == null) {
                draftDO.setCreatedAt(LocalDateTime.now());
            }
            sceneDraftMapper.insert(draftDO);
        }

        return toDomain(draftDO);
    }

    @Override
    public void deleteBySceneId(String sceneId) {
        sceneDraftMapper.delete(
                new LambdaQueryWrapper<SceneDraftDO>()
                        .eq(SceneDraftDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDraftDO::getSceneId, sceneId)
        );
    }

    @Override
    public boolean existsBySceneId(String sceneId) {
        return sceneDraftMapper.selectCount(
                new LambdaQueryWrapper<SceneDraftDO>()
                        .eq(SceneDraftDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(SceneDraftDO::getSceneId, sceneId)
        ) > 0;
    }

    private SceneDraft toDomain(SceneDraftDO draftDO) {
        if (draftDO == null) {
            return null;
        }

        SceneDraft draft = new SceneDraft();
        draft.setSceneId(draftDO.getSceneId());
        draft.setSavedAt(draftDO.getSavedAt());

        try {
            if (draftDO.getContent() != null) {
                SceneDetailDTO content = objectMapper.readValue(
                        draftDO.getContent(),
                        SceneDetailDTO.class
                );
                draft.setContent(content);
                // Get version from content, not from draftDO (which may be null)
                draft.setVersion(content.getVersion());
            } else {
                // Default version if no content
                draft.setVersion(1);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse draft JSON data: {}", e.getMessage(), e);
        }

        return draft;
    }

    private SceneDraftDO toDataObject(SceneDraft draft) {
        SceneDraftDO draftDO = new SceneDraftDO();
        draftDO.setTenantId(DEFAULT_TENANT_ID);
        draftDO.setSceneId(draft.getSceneId());
        // Note: version is stored inside content JSON, not as a separate field

        try {
            if (draft.getContent() != null) {
                draftDO.setContent(objectMapper.writeValueAsString(draft.getContent()));
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize draft data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize draft data", e);
        }

        return draftDO;
    }
}
