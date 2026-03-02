package com.semi.simlogistics.web.infrastructure.persistence.scene.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.semi.simlogistics.web.infrastructure.persistence.scene.entity.SceneDraftDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for scene_drafts table.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@Mapper
public interface SceneDraftMapper extends BaseMapper<SceneDraftDO> {
}
