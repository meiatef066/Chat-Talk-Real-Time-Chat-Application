package com.system.chattalk_serverside.service;

import com.system.chattalk_serverside.model.VerificationCode;
import com.system.chattalk_serverside.repository.VerificationCodeRepository;
import com.system.chattalk_serverside.utils.AppMailService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Transactional
@Service
public class VerificationService {
    private final AppMailService appMailService;
    private final VerificationCodeRepository codeRepository;

    @Autowired
    public VerificationService( AppMailService appMailService, VerificationCodeRepository codeRepository) {
        this.appMailService = appMailService;
        this.codeRepository = codeRepository;
    }

    /*
    send code to email after check if there is previous code or not or it expires or not
    */
    public void sendVerificationCode(String email) {
        Optional<VerificationCode> optional = codeRepository.findByEmail(email);
        if (optional.isPresent()) {
            VerificationCode existing = optional.get();
            if(existing.getLastSentAt()!=null&&
                    Duration.between(existing.getLastSentAt(), LocalDateTime.now()).toSeconds()<60) {
                throw new RuntimeException("Please wait before requesting a new code");
            }
        }
        String rawCode = String.valueOf(new Random().nextInt(900000) + 100000);
        String hashedCode = BCrypt.hashpw(rawCode, BCrypt.gensalt());
        log.info("code for :{}is :{}", email, rawCode);

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setCode(hashedCode);
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        verificationCode.setLastSentAt(LocalDateTime.now());
        codeRepository.save(verificationCode);
        appMailService.sendEmail(email, "Email Verification Code 🍕", "Your verification code is: " + rawCode + "\nIt will expire in 20 minutes.");
    }

    /*
    check if code stored and not expires and match original one
    */
    public boolean VerifyCode(String email, String codeInput) {
        Optional<VerificationCode> optional = codeRepository.findByEmail(email);

        if (optional.isEmpty()) return false;

        VerificationCode stored = optional.get();
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) return false;

        boolean matches = BCrypt.checkpw(codeInput, stored.getCode());

        if (matches) codeRepository.delete(stored);
        return matches;
    }

}
