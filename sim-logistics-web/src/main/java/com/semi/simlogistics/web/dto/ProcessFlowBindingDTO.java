package com.semi.simlogistics.web.dto;

/**
 * Process flow binding in scene.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class ProcessFlowBindingDTO {

    private String flowId;
    private String flowVersion;
    private String entryPointId;
    private boolean enabled;
    private int priority;
    private TriggerConditionDTO triggerCondition;

    public ProcessFlowBindingDTO() {
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    public void setFlowVersion(String flowVersion) {
        this.flowVersion = flowVersion;
    }

    public String getEntryPointId() {
        return entryPointId;
    }

    public void setEntryPointId(String entryPointId) {
        this.entryPointId = entryPointId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public TriggerConditionDTO getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(TriggerConditionDTO triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    @Override
    public String toString() {
        return "ProcessFlowBindingDTO{" +
                "flowId='" + flowId + '\'' +
                ", flowVersion='" + flowVersion + '\'' +
                ", entryPointId='" + entryPointId + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                '}';
    }
}
