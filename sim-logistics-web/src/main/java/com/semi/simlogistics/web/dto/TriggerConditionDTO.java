package com.semi.simlogistics.web.dto;

import java.util.List;
import java.util.Map;

/**
 * Trigger condition for process flow.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class TriggerConditionDTO {

    private String entryPointId;
    private String materialType;
    private String materialGrade;
    private String materialBatch;
    private Map<String, String> materialAttributes;
    private String processTag;
    private List<String> allowedProcessTags;

    public TriggerConditionDTO() {
    }

    public String getEntryPointId() {
        return entryPointId;
    }

    public void setEntryPointId(String entryPointId) {
        this.entryPointId = entryPointId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getMaterialGrade() {
        return materialGrade;
    }

    public void setMaterialGrade(String materialGrade) {
        this.materialGrade = materialGrade;
    }

    public String getMaterialBatch() {
        return materialBatch;
    }

    public void setMaterialBatch(String materialBatch) {
        this.materialBatch = materialBatch;
    }

    public Map<String, String> getMaterialAttributes() {
        return materialAttributes;
    }

    public void setMaterialAttributes(Map<String, String> materialAttributes) {
        this.materialAttributes = materialAttributes;
    }

    public String getProcessTag() {
        return processTag;
    }

    public void setProcessTag(String processTag) {
        this.processTag = processTag;
    }

    public List<String> getAllowedProcessTags() {
        return allowedProcessTags;
    }

    public void setAllowedProcessTags(List<String> allowedProcessTags) {
        this.allowedProcessTags = allowedProcessTags;
    }

    @Override
    public String toString() {
        return "TriggerConditionDTO{" +
                "entryPointId='" + entryPointId + '\'' +
                ", materialType='" + materialType + '\'' +
                ", materialGrade='" + materialGrade + '\'' +
                '}';
    }
}
