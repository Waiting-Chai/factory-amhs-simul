package com.semi.simlogistics.web.infrastructure.persistence.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.semi.simlogistics.web.infrastructure.persistence.model.entity.FileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for files table.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@Mapper
public interface FileMapper extends BaseMapper<FileDO> {
}
