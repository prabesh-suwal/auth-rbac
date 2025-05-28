package com.sb.authenticationrbac.repositories;

import com.sb.authenticationrbac.entities.Branch;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BranchRepository extends MongoRepository<Branch, String> {
    boolean existsByCode(String code);
} 