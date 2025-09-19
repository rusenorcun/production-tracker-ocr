package com.example.uretimveri.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_data")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    // BCrypt hash: response'larda görünmesin
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @Column(name = "pass", nullable = false, length = 60) // BCrypt ~60
    private String password;
    
    @Column(name = "permission", nullable = false)
    private String permission;

    // Controller/Service tarafını "fullName" olarak düzelttik.
    @Column(name = "fullname")
    private String fullName;

    @Column(name = "department")
    private String department;

    /** Null/boş ise USER döndür. */
    public String normalizedRole() {
        return (permission == null || permission.isBlank())
                ? "USER"
                : permission.trim().toUpperCase();
    }

    /** Emniyetli set (trim + büyük harf + boşsa USER). */
    public void setPermissionSafe(String role) {
        this.permission = (role == null || role.isBlank()) ? "USER" : role.trim().toUpperCase();
    }

    /** Trimli setter'lar (HTML formdan boş/whitespace gelebilir) */
    public void setFullName(String s) {
        this.fullName = (s == null || s.isBlank()) ? null : s.trim();
    }

    public void setDepartment(String s) {
        this.department = (s == null || s.isBlank()) ? null : s.trim();
    }
}
