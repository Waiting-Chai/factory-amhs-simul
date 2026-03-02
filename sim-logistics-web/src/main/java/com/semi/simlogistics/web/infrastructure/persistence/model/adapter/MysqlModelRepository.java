package com.semi.simlogistics.web.infrastructure.persistence.model.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.domain.model.Model;
import com.semi.simlogistics.web.domain.model.ModelRepository;
import com.semi.simlogistics.web.dto.ModelMetadataDTO;
import com.semi.simlogistics.web.dto.ModelSummaryDTO;
import com.semi.simlogistics.web.infrastructure.persistence.model.entity.ModelLibraryDO;
import com.semi.simlogistics.web.infrastructure.persistence.model.mapper.ModelLibraryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MySQL-based implementation of ModelRepository.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class MysqlModelRepository implements ModelRepository {

    private static final Logger logger = LoggerFactory.getLogger(MysqlModelRepository.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final ModelLibraryMapper modelLibraryMapper;
    private final ObjectMapper objectMapper;

    public MysqlModelRepository(ModelLibraryMapper modelLibraryMapper, ObjectMapper objectMapper) {
        this.modelLibraryMapper = modelLibraryMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Model> findById(String modelId) {
        ModelLibraryDO modelDO = modelLibraryMapper.selectById(modelId);
        return Optional.ofNullable(toDomain(modelDO));
    }

    @Override
    public Optional<Model> findByName(String name) {
        ModelLibraryDO modelDO = modelLibraryMapper.selectOne(
                new LambdaQueryWrapper<ModelLibraryDO>()
                        .eq(ModelLibraryDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ModelLibraryDO::getName, name)
        );
        return Optional.ofNullable(toDomain(modelDO));
    }

    @Override
    public List<ModelSummaryDTO> findByFilters(int page, int pageSize, String modelType, String status, String keyword) {
        LambdaQueryWrapper<ModelLibraryDO> wrapper = buildListWrapper(modelType, status, keyword);
        IPage<ModelLibraryDO> pageResult = modelLibraryMapper.selectPage(
                new Page<>(page, pageSize),
                wrapper
        );
        return pageResult.getRecords().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long countByFilters(String modelType, String status, String keyword) {
        return modelLibraryMapper.selectCount(buildListWrapper(modelType, status, keyword));
    }

    @Override
    public Model save(Model model) {
        ModelLibraryDO modelDO = toDataObject(model);
        LocalDateTime now = LocalDateTime.now();
        modelDO.setUpdatedAt(now);
        modelDO.setTenantId(DEFAULT_TENANT_ID);

        String persistenceId = model.getId() != null ? model.getId() : model.getModelId();
        ModelLibraryDO existing = persistenceId == null ? null : modelLibraryMapper.selectById(persistenceId);

        if (existing == null) {
            if (modelDO.getId() == null || modelDO.getId().isBlank()) {
                modelDO.setId(persistenceId != null ? persistenceId : UUID.randomUUID().toString());
            }
            if (modelDO.getCreatedAt() == null) {
                modelDO.setCreatedAt(now);
            }
            if (modelDO.getStatus() == null || modelDO.getStatus().isEmpty()) {
                modelDO.setStatus("ENABLED");
            }
            modelLibraryMapper.insert(modelDO);
        } else {
            modelDO.setId(existing.getId());
            modelDO.setCreatedAt(existing.getCreatedAt());
            modelDO.setTenantId(existing.getTenantId());
            if (modelDO.getStatus() == null || modelDO.getStatus().isEmpty()) {
                modelDO.setStatus(existing.getStatus());
            }
            modelLibraryMapper.updateById(modelDO);
        }

        ModelLibraryDO persisted = modelLibraryMapper.selectById(modelDO.getId());
        return toDomain(persisted != null ? persisted : modelDO);
    }

    @Override
    public void deleteById(String modelId) {
        modelLibraryMapper.deleteById(modelId);
    }

    @Override
    public void clearDefaultVersionId(String modelId) {
        modelLibraryMapper.update(
                null,
                new LambdaUpdateWrapper<ModelLibraryDO>()
                        .eq(ModelLibraryDO::getId, modelId)
                        .set(ModelLibraryDO::getDefaultVersionId, null)
                        .set(ModelLibraryDO::getUpdatedAt, LocalDateTime.now())
        );
    }

    @Override
    public void clearDefaultVersionReferences(String versionId) {
        modelLibraryMapper.update(
                null,
                new LambdaUpdateWrapper<ModelLibraryDO>()
                        .eq(ModelLibraryDO::getDefaultVersionId, versionId)
                        .set(ModelLibraryDO::getDefaultVersionId, null)
                        .set(ModelLibraryDO::getUpdatedAt, LocalDateTime.now())
        );
    }

    @Override
    public boolean existsByName(String name) {
        return modelLibraryMapper.selectCount(
                new LambdaQueryWrapper<ModelLibraryDO>()
                        .eq(ModelLibraryDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ModelLibraryDO::getName, name)
        ) > 0;
    }

    @Override
    public boolean existsByNameExcluding(String name, String excludeModelId) {
        return modelLibraryMapper.selectCount(
                new LambdaQueryWrapper<ModelLibraryDO>()
                        .eq(ModelLibraryDO::getTenantId, DEFAULT_TENANT_ID)
                        .eq(ModelLibraryDO::getName, name)
                        .ne(ModelLibraryDO::getId, excludeModelId)
        ) > 0;
    }

    private Model toDomain(ModelLibraryDO modelDO) {
        if (modelDO == null) {
            return null;
        }

        Model model = new Model();
        model.setId(modelDO.getId());
        if (modelDO.getId() != null && !modelDO.getId().isBlank()) {
            model.setModelId(modelDO.getId());
        }
        model.setName(modelDO.getName());
        model.setDescription(modelDO.getDescription());
        model.setModelType(modelDO.getModelType());
        model.setStatus(modelDO.getStatus());
        model.setDefaultVersionId(modelDO.getDefaultVersionId());
        model.setCreatedAt(modelDO.getCreatedAt());
        model.setUpdatedAt(modelDO.getUpdatedAt());
        try {
            if (modelDO.getMetadata() != null && !modelDO.getMetadata().isBlank()) {
                model.setMetadata(objectMapper.readValue(modelDO.getMetadata(), ModelMetadataDTO.class));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse model metadata for {}: {}", modelDO.getId(), e.getMessage());
        }

        return model;
    }

    private ModelLibraryDO toDataObject(Model model) {
        ModelLibraryDO modelDO = new ModelLibraryDO();
        String persistenceId = model.getId() != null ? model.getId() : model.getModelId();
        modelDO.setId(persistenceId);
        modelDO.setName(model.getName());
        modelDO.setDescription(model.getDescription());
        modelDO.setModelType(model.getModelType());
        modelDO.setStatus(model.getStatus());
        modelDO.setDefaultVersionId(model.getDefaultVersionId());
        modelDO.setCreatedAt(model.getCreatedAt());
        modelDO.setUpdatedAt(model.getUpdatedAt());

        try {
            if (model.getMetadata() != null) {
                modelDO.setMetadata(objectMapper.writeValueAsString(model.getMetadata()));
            } else if (modelDO.getMetadata() == null || modelDO.getMetadata().isEmpty()) {
                modelDO.setMetadata(objectMapper.writeValueAsString(new Object()));
            }
        } catch (Exception e) {
            logger.error("Failed to serialize model metadata: {}", e.getMessage(), e);
        }

        return modelDO;
    }

    private ModelSummaryDTO toSummaryDTO(ModelLibraryDO modelDO) {
        ModelSummaryDTO dto = new ModelSummaryDTO();
        dto.setModelId(modelDO.getId());
        dto.setName(modelDO.getName());
        dto.setModelType(modelDO.getModelType());
        dto.setStatus(modelDO.getStatus());
        dto.setCreatedAt(modelDO.getCreatedAt());
        dto.setUpdatedAt(modelDO.getUpdatedAt());

        try {
            if (modelDO.getMetadata() != null) {
                JsonNode metadataNode = objectMapper.readTree(modelDO.getMetadata());
                if (metadataNode.has("thumbnailUrl")) {
                    dto.setThumbnailUrl(metadataNode.get("thumbnailUrl").asText());
                }
                if (metadataNode.has("version")) {
                    dto.setVersion(metadataNode.get("version").asText());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse metadata for model {}: {}", modelDO.getId(), e.getMessage());
        }

        return dto;
    }

    private LambdaQueryWrapper<ModelLibraryDO> buildListWrapper(String modelType, String status, String keyword) {
        LambdaQueryWrapper<ModelLibraryDO> wrapper = new LambdaQueryWrapper<ModelLibraryDO>()
                .eq(ModelLibraryDO::getTenantId, DEFAULT_TENANT_ID);

        if (modelType != null && !modelType.isBlank()) {
            wrapper.eq(ModelLibraryDO::getModelType, modelType.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(ModelLibraryDO::getStatus, status.trim());
        }
        if (keyword != null && !keyword.isBlank()) {
            String query = keyword.trim();
            wrapper.and(w -> w.like(ModelLibraryDO::getName, query)
                    .or()
                    .like(ModelLibraryDO::getDescription, query));
        }
        wrapper.orderByDesc(ModelLibraryDO::getCreatedAt);
        return wrapper;
    }
}
