package com.system.chattalk_serverside.repository;

import com.system.chattalk_serverside.model.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest,Long> {
}
