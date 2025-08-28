package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message,Long> {
    List<Message> findByChat_Id( Long conversationId );

    Page<Message> findByChat_IdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Message findTopByChat_IdOrderByCreatedAtDesc(Long conversationId);

    @Query("""
    select m.chat.id, COUNT(m), c.lastMessage
    from Message m
    join m.chat c
    where m.sender.id <> :userId and m.isRead = false
    group by m.chat.id, c.lastMessage
    """)
    List<Object[]>  countUnreadMessages(@Param("userId") Long userId);

    @Query("""
    select COUNT(m)
    from Message m
    where m.chat.id = :chatId and m.sender.id <> :userId and m.isRead = false
    """)
    Long countUnreadInChatForUser(@Param("chatId") Long chatId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update Message m
    set m.isRead = true
    where m.chat.id = :chatId and m.sender.id <> :userId and m.isRead = false
    """)
    int markConversationAsReadForUser(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
