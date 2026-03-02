package com.semi.simlogistics.web.dto;

import java.util.Map;

/**
 * Entity in scene.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
public class EntityDTO {

    private String id;
    private String type;
    private String name;
    private PositionDTO position;
    private RotationDTO rotation;
    private Map<String, Object> properties;

    public EntityDTO() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PositionDTO getPosition() {
        return position;
    }

    public void setPosition(PositionDTO position) {
        this.position = position;
    }

    public RotationDTO getRotation() {
        return rotation;
    }

    public void setRotation(RotationDTO rotation) {
        this.rotation = rotation;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "EntityDTO{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", position=" + position +
                ", rotation=" + rotation +
                '}';
    }
}
