package com.semi.simlogistics.web.config;

import com.semi.simlogistics.web.SimLogisticsWebApplication;
import io.minio.MinioClient;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal startup validation for core infrastructure beans.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@SpringBootTest(
    classes = SimLogisticsWebApplication.class,
    properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:web_bootstrap;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "sim.redisson.address=redis://127.0.0.1:6379",
        "sim.redisson.lazy-initialization=true",
        "sim.minio.endpoint=http://127.0.0.1:9000",
        "sim.minio.access-key=minioadmin",
        "sim.minio.secret-key=minioadmin"
    }
)
@ActiveProfiles("test")
class InfrastructureConfigTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MinioClient minioClient;

    @Test
    void testInfrastructureBeansCreated() {
        assertThat(dataSource).isNotNull();
        assertThat(sqlSessionFactory).isNotNull();
        assertThat(redissonClient).isNotNull();
        assertThat(minioClient).isNotNull();
    }
}
