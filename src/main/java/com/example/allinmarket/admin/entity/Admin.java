package com.example.allinmarket.admin.entity;

import com.example.allinmarket.admin.enums.AdminRole;
import com.example.allinmarket.common.entity.DeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admins")
public class Admin extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role = AdminRole.ADMIN;

    public static Admin of(String email, String password, String name) {
        Admin admin = new Admin();
        admin.email = email;
        admin.password = password;
        admin.name = name;
        admin.role = AdminRole.ADMIN;
        return admin;
    }
}
