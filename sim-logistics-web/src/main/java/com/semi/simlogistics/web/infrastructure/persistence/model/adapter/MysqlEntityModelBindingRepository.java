package com.semi.simlogistics.web.infrastructure.persistence.model.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.domain.model.EntityModelBinding;
import com.semi.simlogistics.web.domain.model.EntityModelBindingRepository;
import com.semi.simlogistics.web.infrastructure.persistence.model.entity.EntityModelBindingDO;
import com.semi.simlogistics.web.infrastructure.persistence.model.mapper.EntityModelBindingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MySQL-based implementation of EntityModelBindingRepository.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class MysqlEntityModelBindingRepository implements EntityModelBindingRepository {

    private static final Logger logger = LoggerFactory.getLogger(MysqlEntityModelBindingRepository.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final EntityModelBindingMapper bindingMapper;
    private final ObjectMapper objectMapper;

    public MysqlEntityModelBindingRepository(EntityModelBindingMapper bindingMapper, ObjectMapper objectMapper) {
        this.bindingMapper = bindingMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<EntityModelBinding> findById(String bindingId) {
        EntityModelBindingDO bindingDO = bindingMapper.selectById(bindingId);
        return Optional.ofNullable(toDomain(bindingDO));
    }

    @Override
    public List<EntityModelBinding> findBySceneId(String sceneId) {
        List<EntityModelBindingDO> bindingDOList = bindingMapper.selectList(
                new LambdaQueryWrapper<EntityModelBindingDO>()
                        .eq(EntityModelBindingDO::getSceneId, sceneId)
        );
        return bindingDOList.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntityModelBinding> findBySceneIdAndEntityId(String sceneId, String entityId) {
        EntityModelBindingDO bindingDO = bindingMapper.selectOne(
                new LambdaQueryWrapper<EntityModelBindingDO>()
                        .eq(EntityModelBindingDO::getSceneId, sceneId)
                        .eq(EntityModelBindingDO::getEntityId, entityId)
        );
        return Optional.ofNullable(toDomain(bindingDO));
    }

    @Override
    public EntityModelBinding save(EntityModelBinding binding) {
        EntityModelBindingDO bindingDO = toDataObject(binding);
        LocalDateTime now = LocalDateTime.now();
        bindingDO.setTenantId(DEFAULT_TENANT_ID);
        bindingDO.setUpdatedAt(now);

        String businessId = binding.getId();
        EntityModelBindingDO existing = businessId == null ? null : bindingMapper.selectById(businessId);

        if (existing == null) {
            if (bindingDO.getId() == null || bindingDO.getId().isBlank()) {
                bindingDO.setId(UUID.randomUUID().toString());
            }
            if (bindingDO.getCreatedAt() == null) {
                bindingDO.setCreatedAt(now);
            }
            bindingMapper.insert(bindingDO);
        } else {
            bindingDO.setId(existing.getId());
            bindingDO.setCreatedAt(existing.getCreatedAt());
            bindingDO.setTenantId(existing.getTenantId());
            bindingMapper.updateById(bindingDO);
        }

        EntityModelBindingDO persisted = bindingMapper.selectById(bindingDO.getId());
        return toDomain(persisted != null ? persisted : bindingDO);
    }

    @Override
    public void deleteById(String bindingId) {
        bindingMapper.deleteById(bindingId);
    }

    @Override
    public void deleteBySceneId(String sceneId) {
        bindingMapper.delete(
                new LambdaQueryWrapper<EntityModelBindingDO>()
                        .eq(EntityModelBindingDO::getSceneId, sceneId)
        );
    }

    @Override
    public void deleteBySceneIdAndEntityId(String sceneId, String entityId) {
        bindingMapper.delete(
                new LambdaQueryWrapper<EntityModelBindingDO>()
                        .eq(EntityModelBindingDO::getSceneId, sceneId)
                        .eq(EntityModelBindingDO::getEntityId, entityId)
        );
    }

    private EntityModelBinding toDomain(EntityModelBindingDO bindingDO) {
        if (bindingDO == null) {
            return null;
        }

        EntityModelBinding binding = new EntityModelBinding();
        binding.setId(bindingDO.getId());
        binding.setSceneId(bindingDO.getSceneId());
        binding.setEntityId(bindingDO.getEntityId());
        binding.setModelVersionId(bindingDO.getModelVersionId());
        binding.setCreatedAt(bindingDO.getCreatedAt());
        binding.setUpdatedAt(bindingDO.getUpdatedAt());

        try {
            if (bindingDO.getCustomTransform() != null) {
                binding.setCustomTransform(
                        objectMapper.readValue(bindingDO.getCustomTransform(),
                                com.semi.simlogistics.web.dto.TransformConfigDTO.class)
                );
            }
        } catch (Exception e) {
            logger.warn("Failed to parse custom_transform for binding {}: {}", bindingDO.getId(), e.getMessage());
        }

        return binding;
    }

    private EntityModelBindingDO toDataObject(EntityModelBinding binding) {
        EntityModelBindingDO bindingDO = new EntityModelBindingDO();
        bindingDO.setId(binding.getId());
        bindingDO.setSceneId(binding.getSceneId());
        bindingDO.setEntityId(binding.getEntityId());
        bindingDO.setModelVersionId(binding.getModelVersionId());
        bindingDO.setCreatedAt(binding.getCreatedAt());
        bindingDO.setUpdatedAt(binding.getUpdatedAt());

        try {
            if (binding.getCustomTransform() != null) {
                bindingDO.setCustomTransform(objectMapper.writeValueAsString(binding.getCustomTransform()));
            }
        } catch (Exception e) {
            logger.error("Failed to serialize custom_transform: {}", e.getMessage(), e);
        }

        return bindingDO;
    }
}
