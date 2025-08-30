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

    @Query(value = """
    SELECT new com.system.chattalk_serverside.dto.ContactDto.SearchUserResultDTO(
        u.id, u.username, u.email, u.firstName, u.lastName, u.phoneNumber, u.profilePictureUrl, u.isVerified,
        CASE
            WHEN cBlock.id IS NOT NULL THEN 'BLOCKED'
            WHEN cActive.id IS NOT NULL THEN 'ACCEPTED'
            WHEN fr1.id IS NOT NULL OR fr2.id IS NOT NULL THEN 'PENDING'
            ELSE 'NONE'
        END
    )
    FROM User u
    LEFT JOIN Contact cActive ON cActive.user.id = :currentUserId AND cActive.contact.id = u.id AND cActive.status = 'ACTIVE'
    LEFT JOIN Contact cBlock ON cBlock.user.id = :currentUserId AND cBlock.contact.id = u.id AND cBlock.status = 'BLOCKED'
    LEFT JOIN FriendRequest fr1 ON fr1.sender.id = :currentUserId AND fr1.receiver.id = u.id AND fr1.status = 'PENDING'
    LEFT JOIN FriendRequest fr2 ON fr2.sender.id = u.id AND fr2.receiver.id = :currentUserId AND fr2.status = 'PENDING'
    WHERE 
        u.id <> :currentUserId
        AND (
            LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND NOT EXISTS (
            SELECT 1 FROM Contact cEx WHERE cEx.user.id = :currentUserId AND cEx.contact.id = u.id AND cEx.status <> 'DELETED'
        )
    """,
            countQuery = """
    SELECT COUNT(u.id)
    FROM User u
    WHERE 
        u.id <> :currentUserId
        AND (
            LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND NOT EXISTS (
            SELECT 1 FROM Contact cEx WHERE cEx.user.id = :currentUserId AND cEx.contact.id = u.id AND cEx.status <> 'DELETED'
        )
    """)
    Page<SearchUserResultDTO> searchForNewContactsWithStatus(@Param("query") String query, @Param("currentUserId") Long currentUserId, Pageable pageable);
}
