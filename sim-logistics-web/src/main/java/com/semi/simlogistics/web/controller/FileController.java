package com.semi.simlogistics.web.controller;

import com.semi.simlogistics.control.port.storage.ObjectStoragePort;
import com.semi.simlogistics.web.common.ApiEnvelope;
import com.semi.simlogistics.web.infrastructure.persistence.model.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for file download.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-12
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileRepository fileRepository;
    private final ObjectStoragePort objectStoragePort;

    public FileController(FileRepository fileRepository, ObjectStoragePort objectStoragePort) {
        this.fileRepository = fileRepository;
        this.objectStoragePort = objectStoragePort;
    }

    /**
     * GET /api/v1/files/{fileId}/content
     * Get file content by ID.
     *
     * @param fileId file ID
     * @return file content stream
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<?> getFileContent(@PathVariable("fileId") String fileId) {
        logger.debug("GET /api/v1/files/{}/content", fileId);

        // Find file metadata by ID
        var file = fileRepository.findById(fileId);
        if (file.isEmpty()) {
            logger.warn("File not found: fileId={}", fileId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiEnvelope.error("NOT_FOUND", "File not found"));
        }

        var f = file.get();
        String bucket = f.getStorageBucket();
        String key = f.getStorageKey();
        String contentType = f.getFileType();

        logger.debug("Serving file: bucket={}, key={}, contentType={}", bucket, key, contentType);

        // Get object stream from MinIO
        var inputStream = objectStoragePort.getObject(bucket, key);
        if (inputStream == null) {
            logger.error("File metadata exists but object not found in storage: fileId={}, bucket={}, key={}",
                    fileId, bucket, key);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiEnvelope.error("NOT_FOUND", "File content not found"));
        }

        // Return file stream with proper Content-Type
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + f.getFileName() + "\"")
                .body(new InputStreamResource(inputStream, f.getFileName()) {
                    @Override
                    public String getFilename() {
                        return f.getFileName();
                    }

                    @Override
                    public long contentLength() {
                        return f.getFileSize();
                    }
                });
    }
}
