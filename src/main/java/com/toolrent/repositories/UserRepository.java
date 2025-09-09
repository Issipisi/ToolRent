package com.toolrent.repositories;

import com.toolrent.entities.UserEntity;
import com.toolrent.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);  // Encontrar x nombre de usuario
    List<UserEntity> findByRole(UserRole role);           // Lista por rol
    List<UserEntity> findByUsernameContaining(String keyword);  // Búsqueda parcial
}
