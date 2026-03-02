package com.semi.simlogistics.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.domain.model.*;
import com.semi.simlogistics.web.infrastructure.InfrastructureAdapterFactory;
import com.semi.simlogistics.web.infrastructure.persistence.model.FileRepository;
import com.semi.simlogistics.web.infrastructure.persistence.model.adapter.*;
import com.semi.simlogistics.web.infrastructure.persistence.model.mapper.*;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for model management components.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@Configuration
public class ModelConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ModelRepository modelRepository(ModelLibraryMapper modelLibraryMapper, ObjectMapper objectMapper) {
        return new MysqlModelRepository(modelLibraryMapper, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelVersionRepository modelVersionRepository(ModelVersionMapper modelVersionMapper, ObjectMapper objectMapper) {
        return new MysqlModelVersionRepository(modelVersionMapper, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityModelBindingRepository entityModelBindingRepository(EntityModelBindingMapper bindingMapper, ObjectMapper objectMapper) {
        return new MysqlEntityModelBindingRepository(bindingMapper, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileRepository fileRepository(FileMapper fileMapper) {
        return new MysqlFileRepository(fileMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectStoragePort objectStoragePort(MinioClient minioClient, MinioProperties minioProperties) {
        return InfrastructureAdapterFactory.createObjectStoragePort(minioClient, minioProperties.getDefaultBucket());
    }
}
