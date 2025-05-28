package com.sb.authenticationrbac.repositories;

import com.sb.authenticationrbac.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsernameAndActive(String username, boolean active);
    Optional<User> findByIdAndActive(String id, boolean active);
    Optional<User> findByEmailAndActive(String email, boolean active);
}
