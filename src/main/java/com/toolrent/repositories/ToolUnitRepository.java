package com.toolrent.repositories;

import com.toolrent.entities.ToolStatus;
import com.toolrent.entities.ToolUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

//import java.util.List;
import java.util.List;
import java.util.Optional;

public interface ToolUnitRepository extends JpaRepository<ToolUnitEntity, Long> {
    Optional<ToolUnitEntity> findFirstByToolGroupIdAndStatus(Long toolGroupId, ToolStatus status);

    @Query("SELECT u FROM ToolUnitEntity u JOIN FETCH u.toolGroup g")
    List<ToolUnitEntity> findAllWithToolGroup();

    long countByToolGroupIdAndStatusNot(Long toolGroupId, ToolStatus status);
}
