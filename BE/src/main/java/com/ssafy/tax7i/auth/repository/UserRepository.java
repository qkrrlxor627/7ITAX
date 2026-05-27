package com.ssafy.tax7i.auth.repository;

import com.ssafy.tax7i.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCi(String ci);

    Optional<User> findByDi(String di);

    List<User> findByPhoneLast4(String phoneLast4);
}
