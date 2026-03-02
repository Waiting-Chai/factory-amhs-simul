package com.semi.simlogistics.web.infrastructure.persistence.model.adapter;

import com.semi.simlogistics.web.infrastructure.persistence.model.File;
import com.semi.simlogistics.web.infrastructure.persistence.model.FileRepository;
import com.semi.simlogistics.web.infrastructure.persistence.model.entity.FileDO;
import com.semi.simlogistics.web.infrastructure.persistence.model.mapper.FileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * MySQL-based implementation of FileRepository.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class MysqlFileRepository implements FileRepository {

    private static final Logger logger = LoggerFactory.getLogger(MysqlFileRepository.class);
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000000";

    private final FileMapper fileMapper;

    public MysqlFileRepository(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Override
    public Optional<File> findById(String fileId) {
        FileDO fileDO = fileMapper.selectById(fileId);
        return Optional.ofNullable(toDomain(fileDO));
    }

    @Override
    public File save(File file) {
        FileDO fileDO = toDataObject(file);
        LocalDateTime now = LocalDateTime.now();
        fileDO.setTenantId(DEFAULT_TENANT_ID);
        fileDO.setCreatedAt(now);

        String businessId = file.getId();
        FileDO existing = businessId == null ? null : fileMapper.selectById(businessId);

        if (existing == null) {
            if (fileDO.getId() == null || fileDO.getId().isBlank()) {
                fileDO.setId(UUID.randomUUID().toString());
            }
            fileMapper.insert(fileDO);
        } else {
            fileDO.setId(existing.getId());
            fileDO.setTenantId(existing.getTenantId());
            fileMapper.updateById(fileDO);
        }

        FileDO persisted = fileMapper.selectById(fileDO.getId());
        return toDomain(persisted != null ? persisted : fileDO);
    }

    @Override
    public void deleteById(String fileId) {
        fileMapper.deleteById(fileId);
    }

    private File toDomain(FileDO fileDO) {
        if (fileDO == null) {
            return null;
        }

        File file = new File();
        file.setId(fileDO.getId());
        file.setFileName(fileDO.getFileName());
        file.setFileType(fileDO.getFileType());
        file.setFileSize(fileDO.getFileSize());
        file.setStorageBucket(fileDO.getStorageBucket());
        file.setStorageKey(fileDO.getStorageKey());
        file.setStorageUrl(fileDO.getStorageUrl());
        file.setCreatedAt(fileDO.getCreatedAt());

        return file;
    }

    private FileDO toDataObject(File file) {
        FileDO fileDO = new FileDO();
        fileDO.setId(file.getId());
        fileDO.setFileName(file.getFileName());
        fileDO.setFileType(file.getFileType());
        fileDO.setFileSize(file.getFileSize());
        fileDO.setStorageBucket(file.getStorageBucket());
        fileDO.setStorageKey(file.getStorageKey());
        fileDO.setStorageUrl(file.getStorageUrl());
        fileDO.setCreatedAt(file.getCreatedAt());

        return fileDO;
    }
}
