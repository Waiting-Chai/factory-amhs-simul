package com.semi.simlogistics.web.infrastructure.persistence.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Data object for files table.
 *
 * Table: files
 * - id: CHAR(36) PRIMARY KEY
 * - tenant_id: CHAR(36) NOT NULL
 * - file_name: VARCHAR(255) NOT NULL
 * - file_type: VARCHAR(100)
 * - file_size: BIGINT NOT NULL
 * - storage_bucket: VARCHAR(100) NOT NULL
 * - storage_key: VARCHAR(500) NOT NULL
 * - storage_url: VARCHAR(1000)
 * - created_at: DATETIME(3)
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
@TableName("files")
public class FileDO {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("storage_bucket")
    private String storageBucket;

    @TableField("storage_key")
    private String storageKey;

    @TableField("storage_url")
    private String storageUrl;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "FileDO{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileType='" + fileType + '\'' +
                ", fileSize=" + fileSize +
                ", storageBucket='" + storageBucket + '\'' +
                ", storageKey='" + storageKey + '\'' +
                '}';
    }
}
