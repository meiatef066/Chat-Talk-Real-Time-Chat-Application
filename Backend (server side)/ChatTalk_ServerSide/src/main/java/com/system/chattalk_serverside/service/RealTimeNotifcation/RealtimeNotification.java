package com.system.chattalk_serverside.service.RealTimeNotifcation;

import com.system.chattalk_serverside.dto.Entity.NotificationDTO;
import com.system.chattalk_serverside.model.User;

public interface RealtimeNotification {
    void receiveFriendRequestNotification( User toUserId, NotificationDTO notificationDTO);
    void acceptedFriendRequestNotification(User toUserId, NotificationDTO notificationDTO);
    void rejectedFriendRequestNotification(User toUserId, NotificationDTO notificationDTO);
    void receiveNewMessageNotification(User toUserId, NotificationDTO notificationDTO);

}
