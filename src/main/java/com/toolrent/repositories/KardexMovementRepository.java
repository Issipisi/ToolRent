package com.toolrent.repositories;

import com.toolrent.entities.KardexMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KardexMovementRepository extends JpaRepository<KardexMovementEntity, Long> {
}
