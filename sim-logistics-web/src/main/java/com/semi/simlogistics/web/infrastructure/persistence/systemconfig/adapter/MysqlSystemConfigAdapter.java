package com.semi.simlogistics.web.infrastructure.persistence.systemconfig.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.semi.simlogistics.control.port.config.SystemConfigPort;
import com.semi.simlogistics.web.infrastructure.persistence.mybatis.MybatisPlusSessionFactoryProvider;
import com.semi.simlogistics.web.infrastructure.persistence.systemconfig.entity.SystemConfigDO;
import com.semi.simlogistics.web.infrastructure.persistence.systemconfig.mapper.SystemConfigMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

/**
 * MyBatis-Plus adapter for system_config lookup.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public class MysqlSystemConfigAdapter implements SystemConfigPort {

    private final SqlSessionFactory sqlSessionFactory;

    public MysqlSystemConfigAdapter(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = Objects.requireNonNull(sqlSessionFactory, "SqlSessionFactory cannot be null");
    }

    /**
     * Build adapter from datasource with local MyBatis-Plus session factory.
     *
     * @param dataSource data source
     * @return adapter instance
     */
    public static MysqlSystemConfigAdapter fromDataSource(DataSource dataSource) {
        SqlSessionFactory sqlSessionFactory = MybatisPlusSessionFactoryProvider.create(dataSource, SystemConfigMapper.class);
        return new MysqlSystemConfigAdapter(sqlSessionFactory);
    }

    @Override
    public Optional<String> findConfigValue(String tenantId, String key) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SystemConfigMapper mapper = session.getMapper(SystemConfigMapper.class);

            QueryWrapper<SystemConfigDO> queryWrapper = new QueryWrapper<SystemConfigDO>()
                .select("config_value")
                .eq("tenant_id", tenantId)
                .eq("config_key", key)
                .last("LIMIT 1");

            SystemConfigDO config = mapper.selectOne(queryWrapper);
            if (config == null || config.getConfigValue() == null || config.getConfigValue().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(config.getConfigValue());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
