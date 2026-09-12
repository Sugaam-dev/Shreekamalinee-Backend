package com.pmrgsolution.features.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pmrgsolution.features.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email); 
    Boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Boolean existsByPhoneNumber(String phoneNumber);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(u.phoneNumber IS NOT NULL AND LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY u.createdAt DESC")
    List<User> searchUsers(@Param("query") String query);

    List<User> findAllByOrderByCreatedAtDesc();
}