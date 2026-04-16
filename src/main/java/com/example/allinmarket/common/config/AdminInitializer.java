package com.example.allinmarket.common.config;

import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile(("!test"))
@ConditionalOnProperty(name = "app.init.admin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String email;

    @Value("${admin.password}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminRepository.existsByEmail(email)) return; // 이미 존재하면 스킵

        Admin admin = Admin.of(
                email,
                passwordEncoder.encode(password),
                "John Cena"
        );
        adminRepository.save(admin);
        log.info("관리자 계정이 생성되었습니다: {}", email);
    }
}