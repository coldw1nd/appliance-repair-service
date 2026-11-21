package ru.mirea.appliancerepair.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.appliancerepair.backend.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
