package com.semi.simlogistics.web.infrastructure.persistence.systemconfig.adapter;

import com.semi.simlogistics.web.infrastructure.persistence.mybatis.MybatisPlusSessionFactoryProvider;
import com.semi.simlogistics.web.infrastructure.persistence.systemconfig.entity.SystemConfigDO;
import com.semi.simlogistics.web.infrastructure.persistence.systemconfig.mapper.SystemConfigMapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MysqlSystemConfigAdapter.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
class MysqlSystemConfigAdapterTest {

    private static final String TENANT = "00000000-0000-0000-0000-000000000000";

    @Test
    void testFindConfigValue_ReturnsDatabaseValue() {
        PooledDataSource dataSource = createDataSource("web_cfg_hit");
        SqlSessionFactory sqlSessionFactory = createSessionFactory(dataSource);
        insertConfigByMapper(sqlSessionFactory, TENANT, "traffic.replan.timeoutSeconds", "90");

        MysqlSystemConfigAdapter adapter = new MysqlSystemConfigAdapter(sqlSessionFactory);
        assertThat(adapter.findConfigValue(TENANT, "traffic.replan.timeoutSeconds")).contains("90");
    }

    @Test
    void testFindConfigValue_ReturnsEmptyWhenMissing() {
        PooledDataSource dataSource = createDataSource("web_cfg_miss");
        SqlSessionFactory sqlSessionFactory = createSessionFactory(dataSource);

        MysqlSystemConfigAdapter adapter = new MysqlSystemConfigAdapter(sqlSessionFactory);
        assertThat(adapter.findConfigValue(TENANT, "traffic.replan.timeoutSeconds")).isEmpty();
    }

    @Test
    void testCreateFromDataSource_UsesMybatisPlusPipeline() {
        PooledDataSource dataSource = createDataSource("web_cfg_factory");
        SqlSessionFactory sqlSessionFactory = createSessionFactory(dataSource);
        insertConfigByMapper(sqlSessionFactory, TENANT, "traffic.replan.maxAttempts", "5");

        MysqlSystemConfigAdapter adapter = MysqlSystemConfigAdapter.fromDataSource(dataSource);
        assertThat(adapter.findConfigValue(TENANT, "traffic.replan.maxAttempts")).contains("5");
    }

    private PooledDataSource createDataSource(String dbName) {
        return new PooledDataSource(
            "org.h2.Driver",
            "jdbc:h2:mem:" + dbName
                + ";MODE=MySQL"
                + ";DB_CLOSE_DELAY=-1"
                + ";INIT=RUNSCRIPT FROM 'classpath:sql/system_config_schema.sql'",
            "sa",
            ""
        );
    }

    private SqlSessionFactory createSessionFactory(PooledDataSource dataSource) {
        return MybatisPlusSessionFactoryProvider.create(
            dataSource,
            SystemConfigMapper.class
        );
    }

    private void insertConfigByMapper(SqlSessionFactory sqlSessionFactory, String tenantId, String key, String value) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            SystemConfigMapper mapper = session.getMapper(SystemConfigMapper.class);
            SystemConfigDO configDO = new SystemConfigDO();
            configDO.setTenantId(tenantId);
            configDO.setConfigKey(key);
            configDO.setConfigValue(value);
            mapper.insert(configDO);
        }
    }
}
