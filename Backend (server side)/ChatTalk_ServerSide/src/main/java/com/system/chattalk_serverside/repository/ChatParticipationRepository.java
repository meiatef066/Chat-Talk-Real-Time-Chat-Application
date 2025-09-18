package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.ChatParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatParticipationRepository extends JpaRepository<ChatParticipation, Long> {
    
    List<ChatParticipation> findByUserId(Long userId);
    
    List<ChatParticipation> findByUserIdAndStatus(Long userId, ChatParticipation.ParticipationStatus status);
    
    long countByChatId(Long chatId);
    
    long countByChatIdAndRole(Long chatId, ChatParticipation.ParticipationRole role);
    
    List<ChatParticipation> findByChatId(Long chatId);
}
