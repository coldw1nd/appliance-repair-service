package ru.mirea.appliancerepair.backend.security;

import org.junit.jupiter.api.Test;
import ru.mirea.appliancerepair.backend.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Correct-Horse-2026!", "repair@example.org"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordContainingLogin() {
        assertThatThrownBy(() -> policy.validate("Repair-Strong-2026!", "repair@example.org"))
                .isInstanceOf(BadRequestException.class);
    }
}
