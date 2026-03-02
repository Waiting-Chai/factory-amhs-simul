package com.semi.simlogistics.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;

/**
 * Boot entry for sim-logistics-web module.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
@SpringBootApplication(scanBasePackages = {
        "com.semi.simlogistics.web",
        "com.semi.simlogistics.core",
        "com.semi.simlogistics.control"
})
@MapperScan({
        "com.semi.simlogistics.web.infrastructure.persistence.scene.mapper",
        "com.semi.simlogistics.web.infrastructure.persistence.model.mapper",
        "com.semi.simlogistics.web.infrastructure.persistence.systemconfig.mapper"
})
public class SimLogisticsWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimLogisticsWebApplication.class, args);
    }
}
