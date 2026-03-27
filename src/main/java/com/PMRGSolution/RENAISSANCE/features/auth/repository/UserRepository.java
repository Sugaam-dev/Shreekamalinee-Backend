package com.PMRGSolution.RENAISSANCE.features.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.PMRGSolution.RENAISSANCE.features.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Better for production: handles case sensitivity issues
    Optional<User> findByEmailIgnoreCase(String email); 
    Boolean existsByEmailIgnoreCase(String email);
}