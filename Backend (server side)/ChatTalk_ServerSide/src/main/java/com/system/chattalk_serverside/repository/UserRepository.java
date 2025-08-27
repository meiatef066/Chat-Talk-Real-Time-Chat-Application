package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.dto.ContactDto.SearchUserResultDTO;
import com.system.chattalk_serverside.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail( String username );

    boolean existsByEmail( @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email );

    @Query("SELECT new com.system.chattalk_serverside.dto.ContactDto.SearchUserResultDTO(" +
           "u.id, u.username, u.email, u.firstName, u.lastName, u.phoneNumber, " +
           "u.profilePictureUrl, u.isVerified, " +
           "CASE " +
           "  WHEN EXISTS (SELECT 1 FROM FriendRequest fr WHERE fr.sender = u AND fr.receiver.id = :currentUserId AND fr.status = 'ACCEPTED') THEN 'ACCEPTED' " +
           "  WHEN EXISTS (SELECT 1 FROM FriendRequest fr WHERE fr.receiver = u AND fr.sender.id = :currentUserId AND fr.status = 'ACCEPTED') THEN 'ACCEPTED' " +
           "  WHEN EXISTS (SELECT 1 FROM FriendRequest fr WHERE fr.sender = u AND fr.receiver.id = :currentUserId AND fr.status = 'PENDING') THEN 'PENDING' " +
           "  WHEN EXISTS (SELECT 1 FROM FriendRequest fr WHERE fr.receiver = u AND fr.sender.id = :currentUserId AND fr.status = 'PENDING') THEN 'PENDING' " +
           "  ELSE 'NONE' " +
           "END) " +
           "FROM User u " +
           "WHERE u.id != :currentUserId " +
           "AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SearchUserResultDTO> searchForNewContactsWithStatus( @Param("query") String query, @Param("currentUserId") Long currentUserId, Pageable pageable );
}
