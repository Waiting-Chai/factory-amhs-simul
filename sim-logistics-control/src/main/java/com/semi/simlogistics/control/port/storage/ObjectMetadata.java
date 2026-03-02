package com.semi.simlogistics.control.port.storage;

import java.time.Instant;

/**
 * Object metadata view for storage port.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-09
 */
public record ObjectMetadata(
    String bucket,
    String objectKey,
    long size,
    String contentType,
    String etag,
    Instant lastModified
) {
}
