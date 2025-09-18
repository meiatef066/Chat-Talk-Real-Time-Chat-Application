package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n from Notification n
        where n.user.email=:userEmail
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByUserEmail(@Param("userEmail") String email );
    
    List<Notification> findByUserId(Long userId);
}
