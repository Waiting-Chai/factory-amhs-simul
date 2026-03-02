package com.semi.simlogistics.web.infrastructure.persistence.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.semi.simlogistics.web.infrastructure.persistence.model.entity.EntityModelBindingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for entity_model_binding table.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@Mapper
public interface EntityModelBindingMapper extends BaseMapper<EntityModelBindingDO> {
}
