package com.growmighty.lectures.firstday.tangledmonolith.user.infrastructure;

import com.growmighty.lectures.firstday.tangledmonolith.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
