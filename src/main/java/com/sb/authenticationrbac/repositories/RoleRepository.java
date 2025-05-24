package com.sb.authenticationrbac.repositories;

import com.sb.authenticationrbac.entities.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, String> {
}
