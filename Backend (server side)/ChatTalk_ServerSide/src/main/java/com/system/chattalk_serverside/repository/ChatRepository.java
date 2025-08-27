package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * Find private chat between two users with optimized query
     */
    @Query("""
        SELECT c FROM Chat c
        WHERE c.chatType = com.system.chattalk_serverside.enums.ChatType.PRIVATE
        AND EXISTS (
            SELECT 1 FROM ChatParticipation cp1 
            WHERE cp1.chat = c AND cp1.user.id = :user1Id
        )
        AND EXISTS (
            SELECT 1 FROM ChatParticipation cp2 
            WHERE cp2.chat = c AND cp2.user.id = :user2Id
        )
        AND (
            SELECT COUNT(cp3) 
            FROM ChatParticipation cp3 
            WHERE cp3.chat = c
        ) = 2
        """)
    Optional<Chat> findPrivateChatBetweenUsers(@Param("user1Id") Long user1Id,
                                               @Param("user2Id") Long user2Id);

    /**
     * Find all chats where user is a participant
     */
    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        ORDER BY c.updatedAt DESC
        """)
    List<Chat> findChatsByUserId(@Param("userId") Long userId);

    /**
     * Find all private chats where user is a participant
     */
    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        AND c.chatType = com.system.chattalk_serverside.enums.ChatType.PRIVATE
        ORDER BY c.updatedAt DESC
        """)
    List<Chat> findPrivateChatsByUserId(@Param("userId") Long userId);

    /**
     * Find recent chats for user with limit
     */
    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        ORDER BY c.updatedAt DESC
        """)
    List<Chat> findRecentChatsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Search chats by name (case-insensitive)
     */
    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        AND LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ORDER BY c.updatedAt DESC
        """)
    Page<Chat> searchChatsByName(@Param("userId") Long userId, 
                                 @Param("searchTerm") String searchTerm, 
                                 Pageable pageable);

    /**
     * Find chats by type where user is participant
     */
    @Query("""
        SELECT DISTINCT c FROM Chat c
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        AND c.chatType = :chatType
        ORDER BY c.updatedAt DESC
        """)
    List<Chat> findChatsByTypeAndUserId(@Param("userId") Long userId, 
                                        @Param("chatType") String chatType);

    /**
     * Check if user is participant in chat
     */
    @Query("""
        SELECT COUNT(cp) > 0 FROM ChatParticipation cp
        WHERE cp.chat.id = :chatId AND cp.user.id = :userId
        """)
    boolean isUserInChat(@Param("chatId") Long chatId, @Param("userId") Long userId);

    /**
     * Get chat participants
     */
    @Query("""
        SELECT cp.user.email FROM ChatParticipation cp
        WHERE cp.chat.id = :chatId
        """)
    List<String> getChatParticipantEmails(@Param("chatId") Long chatId);

    /**
     * Count user's chats
     */
    @Query("""
        SELECT COUNT(DISTINCT c) FROM Chat c
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        """)
    Long countChatsByUserId(@Param("userId") Long userId);

    /**
     * Find chats with message count
     */
    @Query("""
        SELECT c, COUNT(m) as messageCount FROM Chat c
        LEFT JOIN c.messages m
        JOIN c.participants cp
        WHERE cp.user.id = :userId
        GROUP BY c
        ORDER BY c.updatedAt DESC
        """)
    Page<Object[]> findChatsWithMessageCount(@Param("userId") Long userId, Pageable pageable);
}
