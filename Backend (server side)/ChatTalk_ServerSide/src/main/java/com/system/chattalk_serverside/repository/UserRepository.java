package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail( String username );

    boolean existsByEmail( @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email );
}
