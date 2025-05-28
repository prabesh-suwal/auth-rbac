package com.sb.authenticationrbac.repositories;

import com.sb.authenticationrbac.entities.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {

    List<Permission> findByIdIn(List<String> ids);

    Optional<Permission> findByName(String name);

}