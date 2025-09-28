package com.toolrent.repositories;

import com.toolrent.entities.ToolGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolGroupRepository extends JpaRepository<ToolGroupEntity, Long> {
}

