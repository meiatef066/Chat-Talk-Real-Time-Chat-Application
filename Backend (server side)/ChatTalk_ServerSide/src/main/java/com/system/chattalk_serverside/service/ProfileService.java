package com.system.chattalk_serverside.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.system.chattalk_serverside.dto.AuthDto.UpdateProfileRequest;
import com.system.chattalk_serverside.dto.Entity.UserDTO;
import com.system.chattalk_serverside.exception.UserNotFoundException;
import com.system.chattalk_serverside.model.User;
import com.system.chattalk_serverside.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ProfileService( UserRepository userRepository, Cloudinary cloudinary, PasswordEncoder passwordEncoder ) {
        this.userRepository = userRepository;
        this.cloudinary = cloudinary;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO GetProfile( String email ) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException(email);
        }
        Optional<User> newUser = userRepository.findByEmail(email);
        User user = newUser.get();
        return UserDTO.builder().id(user.getId()).email(email).username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName()).profilePictureUrl(user.getProfilePictureUrl()).phoneNumber(user.getPhoneNumber()).bio(user.getBio()).gender(user.getGender()).dateOfBirth(user.getDateOfBirth()).build();
    }

    public UserDTO UpdateProfile( UpdateProfileRequest request ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Updating profile for user: {}", email);
        log.info("Request data: firstName={}, lastName={}, bio={}, gender={}, dateOfBirth={}, phoneNumber={}", request.getFirstName(), request.getLastName(), request.getBio(), request.getGender(), request.getDateOfBirth(), request.getPhoneNumber());

        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException(email);
        }
        Optional<User> newUser = userRepository.findByEmail(email);
        User user = newUser.get();
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().trim().isEmpty()) {
            log.info("Processing dateOfBirth: {}", request.getDateOfBirth());
            try {
                // Parse the date string to Date object
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                dateFormat.setLenient(false);
                Date parsedDate = dateFormat.parse(request.getDateOfBirth().trim());
                log.info("Successfully parsed date: {}", parsedDate);
                user.setDateOfBirth(parsedDate);
            } catch (java.text.ParseException e) {
                log.error("Invalid date format: {}", request.getDateOfBirth(), e);
                throw new RuntimeException("Invalid date format. Please use yyyy-MM-dd format.");
            }
        } else {
            log.info("dateOfBirth is null or empty, skipping");
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        userRepository.save(user);
        return UserDTO.builder().id(user.getId()).email(user.getEmail()).username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName()).profilePictureUrl(user.getProfilePictureUrl()).phoneNumber(user.getPhoneNumber()).bio(user.getBio()).gender(user.getGender()).dateOfBirth(user.getDateOfBirth()).build();

    }

    public void changePassword( String email, String oldPassword, String newPassword ) {
        log.info("Processing password change for user: {}", email);

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", email);
    }

    public UserDTO UpdateProfilePicture( MultipartFile profilePicture ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Updating profile picture for user: {}", email);

        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException(email);
        }

        Optional<User> newUser = userRepository.findByEmail(email);
        User user = newUser.get();

        try {
            Map uploadResult = cloudinary.uploader().upload(profilePicture.getBytes(), ObjectUtils.emptyMap());
            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Successfully uploaded image to Cloudinary: {}", imageUrl);
            user.setProfilePictureUrl(imageUrl);
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image", e);
        }

        userRepository.save(user);

        return UserDTO.builder().id(user.getId()).email(user.getEmail()).username(user.getUsername()).firstName(user.getFirstName()).lastName(user.getLastName()).profilePictureUrl(user.getProfilePictureUrl()).phoneNumber(user.getPhoneNumber()).bio(user.getBio()).gender(user.getGender()).dateOfBirth(user.getDateOfBirth()).build();
    }


}