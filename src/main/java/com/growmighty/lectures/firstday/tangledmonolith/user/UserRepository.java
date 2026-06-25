package com.growmighty.lectures.firstday.tangledmonolith.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
