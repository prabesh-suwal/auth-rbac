package com.sb.authenticationrbac.repositories;

import com.sb.authenticationrbac.entities.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RoleRepository extends MongoRepository<Role, String> {
    List<Role> findByIdInAndActive(List<String> ids, boolean active);
    List<Role> findByNameInAndActive(List<String> ids, boolean active);
}
