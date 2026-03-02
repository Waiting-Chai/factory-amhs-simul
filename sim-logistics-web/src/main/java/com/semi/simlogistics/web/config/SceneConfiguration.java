package com.semi.simlogistics.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.web.domain.scene.SceneDraftRepository;
import com.semi.simlogistics.web.domain.scene.SceneRepository;
import com.semi.simlogistics.web.infrastructure.persistence.scene.adapter.MysqlSceneDraftRepository;
import com.semi.simlogistics.web.infrastructure.persistence.scene.adapter.MysqlSceneRepository;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneDraftMapper;
import com.semi.simlogistics.web.infrastructure.persistence.scene.mapper.SceneMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for scene management components.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
@Configuration
public class SceneConfiguration {

    @Bean
    public SceneRepository sceneRepository(SceneMapper sceneMapper, ObjectMapper objectMapper) {
        return new MysqlSceneRepository(sceneMapper, objectMapper);
    }

    @Bean
    public SceneDraftRepository sceneDraftRepository(SceneDraftMapper sceneDraftMapper, ObjectMapper objectMapper) {
        return new MysqlSceneDraftRepository(sceneDraftMapper, objectMapper);
    }
}
