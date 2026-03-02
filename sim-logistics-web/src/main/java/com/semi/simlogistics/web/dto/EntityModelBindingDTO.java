package com.semi.simlogistics.web.dto;

/**
 * Entity-model binding information.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class EntityModelBindingDTO {

    private String bindingId;
    private String entityId;
    private String modelId;
    private String versionId;
    private TransformConfigDTO customTransform;

    public EntityModelBindingDTO() {
    }

    public String getBindingId() {
        return bindingId;
    }

    public void setBindingId(String bindingId) {
        this.bindingId = bindingId;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public TransformConfigDTO getCustomTransform() {
        return customTransform;
    }

    public void setCustomTransform(TransformConfigDTO customTransform) {
        this.customTransform = customTransform;
    }
}
