package com.system.chattalk_serverside.service.Connections;


import com.system.chattalk_serverside.service.RealTimeNotifcation.RealtimeNotificationImpl;
import com.system.chattalk_serverside.dto.ContactDto.FriendRequestResponse;
import com.system.chattalk_serverside.dto.ContactDto.PendingFriendRequestDto;
import com.system.chattalk_serverside.dto.Entity.NotificationDTO;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.FriendRequestRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Service
public class ContactServiceImpl  implements ContactService{
    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final RealtimeNotificationImpl realtimeNotificationImpl;

    public ContactServiceImpl( UserRepository userRepository, FriendRequestRepository friendRequestRepository, RealtimeNotificationImpl realtimeNotificationImpl ) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.realtimeNotificationImpl = realtimeNotificationImpl;
    }

    /**
     * Send a friend request from the authenticated user to a receiver by email
     */
    @Override
    public FriendRequestResponse sendFriendRequest( String receiverEmail ) {
        validateEmail(receiverEmail, "Receiver email");

        String senderEmail = getAuthenticatedEmail();
        if (senderEmail.equals(receiverEmail)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + receiverEmail));

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + senderEmail));

        validateFriendshipStatus(receiver, sender);

        FriendRequest friendRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequest.RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);
        log.info("Friend request sent from {} to {}", senderEmail, receiverEmail);

        // Publish event

        // Send notification
        // notify user
        realtimeNotificationImpl.receiveFriendRequestNotification(receiver, NotificationDTO.builder()
                        .userId(receiver.getId())
                .build());

        return FriendRequestResponse.builder()
                .requestId(savedRequest.getId())
                .sender(senderEmail)
                .receiver(receiverEmail)
                .status(savedRequest.getStatus().name())
                .build();
    }

    @Override
    public List<UserDTO> getFriends() {
        String email = getAuthenticatedEmail();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        List<FriendRequest> acceptedAsReceiver = friendRequestRepository.findByReceiverAndStatus(currentUser, FriendRequest.RequestStatus.ACCEPTED);
        List<FriendRequest> acceptedAsSender = friendRequestRepository.findBySenderAndStatus(currentUser, FriendRequest.RequestStatus.ACCEPTED);

        return Stream.concat(acceptedAsReceiver.stream(), acceptedAsSender.stream())
                .map(req -> currentUser.equals(req.getSender()) ? req.getReceiver() : req.getSender())
                .distinct()
                .map(this::toUserDTO)
                .toList();
    }

    @Override
    public List<PendingFriendRequestDto> getPendingRequests() {
        String receiverEmail = getAuthenticatedEmail();

        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + receiverEmail));

        // Sort by newest first
        return friendRequestRepository.findByReceiverAndStatus(receiver, FriendRequest.RequestStatus.PENDING)
                .stream()
                .sorted(Comparator.comparing(FriendRequest::getCreatedAt).reversed())
                .map(this::toPendingRequestDto)
                .toList();
    }

    @Transactional
    @Override
    public void acceptRequest( Long requestId ) {
        FriendRequest request = getValidPendingRequestForUser(requestId);

        request.setStatus(FriendRequest.RequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        log.info("Friend request accepted: {}", requestId);
        //event
        realtimeNotificationImpl.acceptedFriendRequestNotification(request.getSender(), NotificationDTO.builder()
                .userId(request.getSender().getId())
                .build());
    }
    /**
     * Reject a friend request
     */
    @Transactional
    @Override
    public void rejectRequest( Long requestId ) {
        FriendRequest request = getValidPendingRequestForUser(requestId);

        request.setStatus(FriendRequest.RequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        log.info("Friend request rejected: {}", requestId);
        //event
        realtimeNotificationImpl.rejectedFriendRequestNotification(request.getSender(), NotificationDTO.builder()
                .userId(request.getSender().getId())
                .build());
    }


    private FriendRequest getValidPendingRequestForUser( Long requestId ) {
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }

        String currentUserEmail = getAuthenticatedEmail();

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!request.getReceiver().getEmail().equals(currentUserEmail)) {
            throw new RuntimeException("Not authorized to handle this friend request");
        }
        if (request.getStatus() != FriendRequest.RequestStatus.PENDING) {
            throw new RuntimeException("Friend request is not pending");
        }
        return request;
    }
    /**
     * Convert FriendRequest to PendingFriendRequestDto
     */
    private PendingFriendRequestDto toPendingRequestDto( FriendRequest request) {
        return PendingFriendRequestDto.builder()
                .requestId(request.getId())
                .email(request.getSender().getEmail())
                .firstName(request.getSender().getFirstName())
                .lastName(request.getSender().getLastName())
                .profilePictureUrl(request.getSender().getProfilePictureUrl())
                .bio(request.getSender().getBio())
                .isOnline(request.getSender().getIsOnline())
                .requestDate(request.getCreatedAt())
                .build();
    }

    private void validateEmail(String email, String fieldName) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
    private String getAuthenticatedEmail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("No authenticated user found");
        }
        return email;
    }


    private void validateFriendshipStatus( User receiver, User sender ) {
        // Check if friend request already exists
        if (friendRequestRepository.existsBySenderAndReceiverAndStatus(sender, receiver, FriendRequest.RequestStatus.PENDING)) {
            throw new RuntimeException("Friend request already sent and pending");
        }
        // Check if friend request already exists
        if (friendRequestRepository.existsBySenderAndReceiverAndStatus(receiver, sender, FriendRequest.RequestStatus.PENDING)) {
            throw new RuntimeException(receiver.getEmail() + " already sent you a request. Check your pending list.");
        }
        // already friends
        boolean alreadyFriends =
                friendRequestRepository.existsBySenderAndReceiverAndStatus(sender, receiver, FriendRequest.RequestStatus.ACCEPTED) ||
                friendRequestRepository.existsBySenderAndReceiverAndStatus(receiver, sender, FriendRequest.RequestStatus.ACCEPTED);
        if (alreadyFriends) {
            throw new RuntimeException("You are already friends");
        }
    }
    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .gender(user.getGender())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .build();
    }
}
