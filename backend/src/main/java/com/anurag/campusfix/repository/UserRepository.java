package com.anurag.campusfix.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.anurag.campusfix.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    org.springframework.data.domain.Page<UserEntity> findByRole(com.anurag.campusfix.entity.Role role, org.springframework.data.domain.Pageable pageable);

    java.util.List<UserEntity> findByRole(com.anurag.campusfix.entity.Role role);

    java.util.List<UserEntity> findByRoleAndIsActiveTrue(com.anurag.campusfix.entity.Role role);
}