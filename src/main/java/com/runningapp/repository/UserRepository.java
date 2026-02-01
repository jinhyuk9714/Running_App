package com.runningapp.repository;

import com.runningapp.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 레포지토리 (Spring Data JPA)
 *
 * JpaRepository 상속 시 기본 CRUD 메서드 자동 제공:
 * - save(), findById(), findAll(), deleteById() 등
 *
 * 메서드 이름 규칙(Query Derivation): findBy + 필드명 → SELECT 쿼리 자동 생성
 * - findByEmail: WHERE email = ?
 * - existsByEmail: SELECT COUNT(*) > 0 WHERE email = ?
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
