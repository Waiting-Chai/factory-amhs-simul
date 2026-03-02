package com.semi.simlogistics.web.dto;

/**
 * Request to set or update entity-model binding.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-11
 */
public class SetEntityModelBindingRequestDTO {

    private String modelVersionId;
    private String modelId;
    private String versionId;

    private TransformConfigDTO customTransform;

    public SetEntityModelBindingRequestDTO() {
    }

    public String getModelVersionId() {
        return modelVersionId;
    }

    public void setModelVersionId(String modelVersionId) {
        this.modelVersionId = modelVersionId;
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
