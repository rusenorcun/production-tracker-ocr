package com.example.uretimveri.config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.example.uretimveri.model.User;
import com.example.uretimveri.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// Tek sefer çalıştır, sonra KALDIR ya da property ile kapat
@Configuration
@ConditionalOnProperty(name = "user.password.migration", havingValue = "true", matchIfMissing = false)
public class PasswordMigrationRunner {

    @Bean
    CommandLineRunner migratePasswords(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            int migrated = 0, skipped = 0;
            for (User u : repo.findAll()) {
                String p = u.getPassword();
                if (p == null || p.isBlank() || isBcrypt(p)) { skipped++; continue; }

                u.setPassword(encoder.encode(p));                 // düz metni BCrypt'e çevir
                if (u.getPermission() != null)                    // rolü normalize et (USER/ADMIN)
                    u.setPermission(u.getPermission().trim().toUpperCase());

                repo.save(u);
                migrated++;
            }
            System.out.printf("[PASS-MIGRATION] migrated=%d, skipped=%d%n", migrated, skipped);
        };
    }

    private boolean isBcrypt(String p) {
        return p.startsWith("$2a$") || p.startsWith("$2b$") || p.startsWith("$2y$");
    }
}
