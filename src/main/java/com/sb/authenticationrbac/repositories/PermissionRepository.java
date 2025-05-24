package com.sb.authenticationrbac.repositories;

import com.sb.authenticationrbac.permission.entity.Permission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends MongoRepository<Permission, String> {
} 