package com.semi.simlogistics.web.infrastructure.persistence.mybatis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;

/**
 * Utility for building local MyBatis-Plus SqlSessionFactory.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public final class MybatisPlusSessionFactoryProvider {

    private MybatisPlusSessionFactoryProvider() {
    }

    /**
     * Create SqlSessionFactory with explicit mapper registration.
     *
     * @param dataSource datasource
     * @param mapperTypes mapper interfaces
     * @return sql session factory
     */
    @SafeVarargs
    public static SqlSessionFactory create(DataSource dataSource, Class<?>... mapperTypes) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(
            new Environment("sim-logistics-web-mybatis", new JdbcTransactionFactory(), dataSource)
        );

        for (Class<?> mapperType : mapperTypes) {
            configuration.addMapper(mapperType);
        }

        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }
}
