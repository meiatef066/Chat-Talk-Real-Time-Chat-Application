package com.system.chattalk_serverside.UnitTest;

import com.system.chattalk_serverside.dto.ContactDto.FriendRequestResponse;
import com.system.chattalk_serverside.dto.ContactDto.PendingFriendRequestDto;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.FriendRequest;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.FriendRequestRepository;
import com.system.chattalk_serverside.repository.UserRepository;
import com.system.chattalk_serverside.service.Connections.ContactServiceImpl;
import com.system.chattalk_serverside.RealTimeNotifcation.RealtimeNotificationImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContactServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendRequestRepository friendRequestRepository;
    @Mock private RealtimeNotificationImpl realtimeNotificationImpl;

    @InjectMocks private ContactServiceImpl contactService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sender = User.builder().id(1L).email("john@example.com").firstName("John").build();
        receiver = User.builder().id(2L).email("jane@example.com").firstName("Jane").build();

        // Set authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sender.getEmail(), null)
        );
    }

    @Test
    void sendFriendRequest_success() {
        when(userRepository.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
        when(userRepository.findByEmail(receiver.getEmail())).thenReturn(Optional.of(receiver));
        when(friendRequestRepository.existsBySenderAndReceiverAndStatus(any(), any(), any())).thenReturn(false);
        when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> {
                    FriendRequest req = invocation.getArgument(0);
                    req.setId(100L);
                    return req;
                });

        FriendRequestResponse response = contactService.sendFriendRequest(receiver.getEmail());

        assertThat(response).isNotNull();
        assertThat(response.getRequestId()).isEqualTo(100L);
        assertThat(response.getSender()).isEqualTo(sender.getEmail());
        assertThat(response.getReceiver()).isEqualTo(receiver.getEmail());

        verify(friendRequestRepository, times(1)).save(any());
    }

    @Test
    void sendFriendRequest_userNotFound_throwsException() {
        when(userRepository.findByEmail(receiver.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.sendFriendRequest(receiver.getEmail()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getFriends_returnsBothSenderAndReceiver() {
        FriendRequest asSender = FriendRequest.builder()
                .id(1L).sender(sender).receiver(receiver)
                .status(FriendRequest.RequestStatus.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();

        FriendRequest asReceiver = FriendRequest.builder()
                .id(2L).sender(receiver).receiver(sender)
                .status(FriendRequest.RequestStatus.ACCEPTED)
                .createdAt(LocalDateTime.now()).build();

        when(userRepository.findByEmail(sender.getEmail())).thenReturn(Optional.of(sender));
        when(friendRequestRepository.findByReceiverAndStatus(sender, FriendRequest.RequestStatus.ACCEPTED))
                .thenReturn(List.of(asReceiver));
        when(friendRequestRepository.findBySenderAndStatus(sender, FriendRequest.RequestStatus.ACCEPTED))
                .thenReturn(List.of(asSender));

        List<UserDTO> friends = contactService.getFriends();

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getEmail()).isEqualTo(receiver.getEmail());
    }

    @Test
    void acceptRequest_success() {
        FriendRequest request = FriendRequest.builder()
                .id(5L).sender(receiver).receiver(sender)
                .status(FriendRequest.RequestStatus.PENDING).build();

        when(friendRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenReturn(request);

        contactService.acceptRequest(5L);

        assertThat(request.getStatus()).isEqualTo(FriendRequest.RequestStatus.ACCEPTED);
        verify(friendRequestRepository).save(request);
    }

    @Test
    void rejectRequest_success() {
        FriendRequest request = FriendRequest.builder()
                .id(6L).sender(receiver).receiver(sender)
                .status(FriendRequest.RequestStatus.PENDING).build();

        when(friendRequestRepository.findById(6L)).thenReturn(Optional.of(request));
        when(friendRequestRepository.save(request)).thenReturn(request);

        contactService.rejectRequest(6L);

        assertThat(request.getStatus()).isEqualTo(FriendRequest.RequestStatus.REJECTED);
        verify(friendRequestRepository).save(request);
    }
}
