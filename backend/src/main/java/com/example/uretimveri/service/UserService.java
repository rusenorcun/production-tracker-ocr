package com.example.uretimveri.service;

import com.example.uretimveri.model.User;
import com.example.uretimveri.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {
    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "OPERATOR", "ADMIN");
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // BCrypt

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Kayıt: her zaman USER rolü verilir. Eski imzayla uyum için 3 parametreli sürüm de var. */
    public boolean register(String username, String rawPassword) {
        return register(username, rawPassword, null);
    }

    /** Eski Controller imzası için: permission gelse de YOK SAYILIR. */
    @Transactional
    public boolean register(String username, String rawPassword, String ignoredPermission) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Kullanıcı adı boş olamaz.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Şifre boş olamaz.");
        }
        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Şifre en az 6 karakter olmalı.");
        }

        String uname = username.trim();
        
        // Kullanıcı adı kontrolü - daha güvenli
        if (userRepository.existsByUsername(uname)) {
            return false; // Controller "Bu kullanıcı adı zaten mevcut!" mesajını gösterecek
        }

        try {
            User u = new User();
            u.setUsername(uname);
            u.setPassword(passwordEncoder.encode(rawPassword)); // BCrypt
            u.setPermissionSafe("USER"); // <-- kritik: her zaman USER

            userRepository.save(u);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent kayıt durumunda unique constraint ihlali
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kayıtlı.");
        } catch (Exception ex) {
            throw new RuntimeException("Kayıt sırasında beklenmeyen hata: " + ex.getMessage(), ex);
        }
    }

    /** Basit kimlik doğrulama kontrolü gerekiyorsa. */
    public boolean verifyCredentials(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return false;
        }
        
        Optional<User> u = userRepository.findByUsername(username.trim());
        return u.isPresent() && passwordEncoder.matches(rawPassword, u.get().getPassword());
    }

    /** Profil bilgisi güncelleme (fullname/department) */
    @Transactional
    public void updateProfileInfo(Long userId, String fullName, String department){
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));
        u.setFullName(fullName);
        u.setDepartment(department);
        userRepository.save(u);
    }

    /** Şifre değiştirme (mevcut şifre doğrulanır) */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword){
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Yeni şifre boş olamaz.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Yeni şifre en az 6 karakter olmalı.");
        }
        
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        if (!passwordEncoder.matches(currentPassword, u.getPassword())) {
            throw new IllegalArgumentException("Mevcut şifre hatalı.");
        }
        
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }

    @Transactional(readOnly = true)
    public User getByUsername(String username){
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Kullanıcı adı boş olamaz.");
        }
        return userRepository.findByUsername(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + username));
    }
    
    @Transactional
    public void updatePermission(Long userId, String newPermission) {
        // Null/boş ise USER'a düşür ve normalize et
        String role = (newPermission == null || newPermission.isBlank())
                ? "USER"
                : newPermission.trim().toUpperCase();

        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Geçersiz rol: " + role);
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı"));

        // Entity tarafındaki güvenli setter: trim + upper + boşsa USER
        user.setPermissionSafe(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().toList(); // JPA reposu null eleman içeren liste döndürmez.
    }

    @Transactional
    public void changeUserRole(String actorUsername, Long targetUserId, String newPermissionRaw) {
        String newRole = normalizeRole(newPermissionRaw);
        if (!ALLOWED_ROLES.contains(newRole)) {
            throw new IllegalArgumentException("Geçersiz rol: " + newRole);
        }

        User actor = userRepository.findByUsername(actorUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Aktör kullanıcı bulunamadı."));

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Hedef kullanıcı bulunamadı."));

        String currentRole = target.normalizedRole();

        // Son ADMIN'i düşürme kontrolü
        if ("ADMIN".equals(currentRole) && !"ADMIN".equals(newRole)) {
            long adminCount = userRepository.countByPermissionIgnoreCase("ADMIN");
            if (adminCount <= 1) {
                throw new IllegalStateException("Sistemde en az bir ADMIN kalmalı.");
            }
            if (actor.getId().equals(target.getId())) {
                throw new IllegalStateException("Kendi ADMIN yetkinizi düşüremezsiniz.");
            }
        }

        target.setPermissionSafe(newRole); // güvenli setter kullan
        userRepository.save(target);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "USER";
        return role.trim().toUpperCase();
    }

    @Transactional
    public void deleteUser(String actorUsername, Long targetUserId) {
        User actor = userRepository.findByUsername(actorUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Aktör kullanıcı bulunamadı."));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Hedef kullanıcı bulunamadı."));

        if (actor.getId().equals(target.getId())) {
            throw new IllegalStateException("Kendi hesabınızı silemezsiniz.");
        }

        // Son ADMIN'i silme kontrolü (tek kontrol yeterli)
        if ("ADMIN".equalsIgnoreCase(target.normalizedRole())) {
            long adminCount = userRepository.countByPermissionIgnoreCase("ADMIN");
            if (adminCount <= 1) {
                throw new IllegalStateException("Sistemde en az bir ADMIN kalmalı.");
            }
        }

        userRepository.delete(target);
    }
}
