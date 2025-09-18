package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest,Long> {
    List<FriendRequest> findByReceiverAndStatus( User currentUser, FriendRequest.RequestStatus requestStatus );

    List<FriendRequest> findBySenderAndStatus( User currentUser, FriendRequest.RequestStatus requestStatus );

    boolean existsBySenderAndReceiverAndStatus( User userSender, User userReceiver, FriendRequest.RequestStatus requestStatus );
    
    Optional<FriendRequest> findBySenderAndReceiverAndStatus( User userSender, User userReceiver, FriendRequest.RequestStatus requestStatus );
    
    List<FriendRequest> findBySenderId(Long senderId);
    
    List<FriendRequest> findByReceiverId(Long receiverId);
}
