package com.toolrent.config;

import com.toolrent.entities.ToolEntity;

public interface ToolCountDTO {
    ToolEntity getTool();
    Long getTotal();
}