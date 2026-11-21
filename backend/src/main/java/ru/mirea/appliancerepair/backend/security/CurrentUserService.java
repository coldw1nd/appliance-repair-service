package ru.mirea.appliancerepair.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.mirea.appliancerepair.backend.domain.UserAccount;
import ru.mirea.appliancerepair.backend.error.AuthenticationFailedException;
import ru.mirea.appliancerepair.backend.repository.UserAccountRepository;

@Component
public class CurrentUserService {
    private final UserAccountRepository users;

    public CurrentUserService(UserAccountRepository users) {
        this.users = users;
    }

    public UserAccount requireEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationFailedException();
        }
        return users.findById(principal.id()).filter(UserAccount::isEnabled)
                .orElseThrow(AuthenticationFailedException::new);
    }

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationFailedException();
        }
        return principal;
    }
}
