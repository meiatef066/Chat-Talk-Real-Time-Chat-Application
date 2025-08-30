package com.system.chattalk_serverside.service;

import com.system.chattalk_serverside.dto.ContactDto.SearchUserResultDTO;
import com.system.chattalk_serverside.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserSearchService {
    private final UserRepository userRepository;

    @Autowired
    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Page<SearchUserResultDTO> searchForNewFriends(String query, Long currentUserId, int page, int size) {
        System.out.println("query: " + query);
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.searchForNewContactsWithStatus(query, currentUserId, pageable);
    }


}
