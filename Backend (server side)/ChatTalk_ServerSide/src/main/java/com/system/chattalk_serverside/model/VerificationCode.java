package com.system.chattalk_serverside.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@Entity
@Table(name = "verification-code")
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode{
    @Id
    private String email;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime lastSentAt;
}