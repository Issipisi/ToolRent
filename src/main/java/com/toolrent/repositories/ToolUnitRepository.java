package com.toolrent.repositories;

import com.toolrent.entities.ToolStatus;
import com.toolrent.entities.ToolUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToolUnitRepository extends JpaRepository<ToolUnitEntity, Long> {
    Optional<ToolUnitEntity> findFirstByToolGroupIdAndStatus(Long toolGroupId, ToolStatus status);

    List<ToolUnitEntity> findAllByToolGroupIdAndStatus(Long toolGroupId, ToolStatus status);
}
