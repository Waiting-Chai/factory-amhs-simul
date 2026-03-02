package com.semi.simlogistics.web.infrastructure.persistence.model.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.domain.model.ModelVersion;
import com.semi.simlogistics.web.domain.model.ModelVersionRepository;
import com.semi.simlogistics.web.infrastructure.persistence.model.entity.ModelVersionDO;
import com.semi.simlogistics.web.infrastructure.persistence.model.mapper.ModelVersionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MySQL-based implementation of ModelVersionRepository.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class MysqlModelVersionRepository implements ModelVersionRepository {

    private static final Logger logger = LoggerFactory.getLogger(MysqlModelVersionRepository.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final ModelVersionMapper modelVersionMapper;
    private final ObjectMapper objectMapper;

    public MysqlModelVersionRepository(ModelVersionMapper modelVersionMapper, ObjectMapper objectMapper) {
        this.modelVersionMapper = modelVersionMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ModelVersion> findById(String versionId) {
        ModelVersionDO versionDO = modelVersionMapper.selectById(versionId);
        return Optional.ofNullable(toDomain(versionDO));
    }

    @Override
    public List<ModelVersion> findByModelId(String modelId) {
        List<ModelVersionDO> versionDOList = modelVersionMapper.selectList(
                new LambdaQueryWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getModelId, modelId)
                        .orderByDesc(ModelVersionDO::getCreatedAt)
        );
        return versionDOList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ModelVersion> findDefaultByModelId(String modelId) {
        ModelVersionDO versionDO = modelVersionMapper.selectOne(
                new LambdaQueryWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getModelId, modelId)
                        .eq(ModelVersionDO::getIsDefault, true)
        );
        return Optional.ofNullable(toDomain(versionDO));
    }

    @Override
    public ModelVersion save(ModelVersion version) {
        ModelVersionDO versionDO = toDataObject(version);
        LocalDateTime now = LocalDateTime.now();
        versionDO.setTenantId(DEFAULT_TENANT_ID);

        String businessId = version.getVersionId() != null ? version.getVersionId() : version.getId();
        ModelVersionDO existing = businessId == null ? null : modelVersionMapper.selectById(businessId);

        if (existing == null) {
            if (versionDO.getId() == null || versionDO.getId().isBlank()) {
                versionDO.setId(businessId != null ? businessId : UUID.randomUUID().toString());
            }
            if (versionDO.getCreatedAt() == null) {
                versionDO.setCreatedAt(now);
            }
            if (versionDO.getStatus() == null || versionDO.getStatus().isEmpty()) {
                versionDO.setStatus("ACTIVE");
            }
            if (versionDO.getIsDefault() == null) {
                versionDO.setIsDefault(false);
            }
            modelVersionMapper.insert(versionDO);
        } else {
            versionDO.setId(existing.getId());
            versionDO.setCreatedAt(existing.getCreatedAt());
            versionDO.setTenantId(existing.getTenantId());
            if (versionDO.getStatus() == null || versionDO.getStatus().isEmpty()) {
                versionDO.setStatus(existing.getStatus());
            }
            if (versionDO.getIsDefault() == null) {
                versionDO.setIsDefault(existing.getIsDefault());
            }
            modelVersionMapper.updateById(versionDO);
        }

        ModelVersionDO persisted = modelVersionMapper.selectById(versionDO.getId());
        return toDomain(persisted != null ? persisted : versionDO);
    }

    @Override
    public void deleteById(String versionId) {
        modelVersionMapper.deleteById(versionId);
    }

    @Override
    public void setDefaultVersion(String modelId, String versionId) {
        modelVersionMapper.update(null,
                new LambdaUpdateWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getModelId, modelId)
                        .set(ModelVersionDO::getIsDefault, false)
        );

        modelVersionMapper.update(null,
                new LambdaUpdateWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getId, versionId)
                        .set(ModelVersionDO::getIsDefault, true)
        );
    }

    @Override
    public long countByModelId(String modelId) {
        return modelVersionMapper.selectCount(
                new LambdaQueryWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getModelId, modelId)
        );
    }

    @Override
    public long countByFileId(String fileId) {
        return modelVersionMapper.selectCount(
                new LambdaQueryWrapper<ModelVersionDO>()
                        .eq(ModelVersionDO::getFileId, fileId)
        );
    }

    private ModelVersion toDomain(ModelVersionDO versionDO) {
        if (versionDO == null) {
            return null;
        }

        ModelVersion version = new ModelVersion();
        version.setId(versionDO.getId());
        version.setVersionId(versionDO.getId());
        version.setModelId(versionDO.getModelId());
        version.setVersion(versionDO.getVersion());
        version.setFileId(versionDO.getFileId());
        version.setDefault(versionDO.getIsDefault() != null ? versionDO.getIsDefault() : false);
        version.setStatus(versionDO.getStatus());
        version.setCreatedAt(versionDO.getCreatedAt());

        try {
            if (versionDO.getTransformConfig() != null) {
                version.setTransformConfig(
                        objectMapper.readValue(versionDO.getTransformConfig(),
                                com.semi.simlogistics.web.dto.TransformConfigDTO.class)
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to parse transform_config for version {}: {}", versionDO.getId(), e.getMessage());
        }

        return version;
    }

    private ModelVersionDO toDataObject(ModelVersion version) {
        ModelVersionDO versionDO = new ModelVersionDO();
        String persistenceId = version.getVersionId() != null ? version.getVersionId() : version.getId();
        versionDO.setId(persistenceId);
        versionDO.setModelId(version.getModelId());
        versionDO.setVersion(version.getVersion());
        versionDO.setFileId(version.getFileId());
        versionDO.setIsDefault(version.isDefault());
        versionDO.setStatus(version.getStatus());
        versionDO.setCreatedAt(version.getCreatedAt());

        try {
            if (version.getTransformConfig() != null) {
                versionDO.setTransformConfig(objectMapper.writeValueAsString(version.getTransformConfig()));
            }
        } catch (Exception e) {
            logger.error("Failed to serialize transform_config: {}", e.getMessage(), e);
        }

        return versionDO;
    }
}
