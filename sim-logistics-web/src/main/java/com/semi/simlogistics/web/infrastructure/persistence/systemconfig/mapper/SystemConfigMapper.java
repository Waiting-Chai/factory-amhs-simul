package com.semi.simlogistics.web.infrastructure.persistence.systemconfig.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.semi.simlogistics.web.infrastructure.persistence.systemconfig.entity.SystemConfigDO;

/**
 * Mapper for system_config table.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigDO> {
}
