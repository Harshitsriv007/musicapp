package com.musicapp.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.musicapp.domain.AuthSession;
import com.musicapp.domain.UserAccount;
import com.musicapp.repository.AuthSessionRepository;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final AuthSessionRepository sessions = mock(AuthSessionRepository.class);
    private final LikedSongRepository likes = mock(LikedSongRepository.class);
    private final PlayHistoryRepository history = mock(PlayHistoryRepository.class);
    private static final Duration TTL = Duration.ofHours(4);
    private final AccountService service =
            new AccountService(users, sessions, likes, history, Clock.fixed(NOW, ZoneOffset.UTC), true, TTL);

    private static UserAccount account(long id, String username, String password) {
        UserAccount account = new UserAccount(username, new BCryptPasswordEncoder().encode(password), NOW);
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    @Test
    void signUpHashesPasswordStoresOnlyTokenHashAndMergesDevice() {
        when(users.saveAndFlush(any())).thenAnswer(inv -> {
            UserAccount a = inv.getArgument(0);
            ReflectionTestUtils.setField(a, "id", 7L);
            return a;
        });

        Session session = service.signUp("  Riya ", "secret-123", "device1");

        ArgumentCaptor<UserAccount> saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("riya");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("secret-123").startsWith("$2");

        assertThat(session.username()).isEqualTo("riya");
        assertThat(session.accountId()).isEqualTo(7L);
        assertThat(session.expiresAt()).isEqualTo(NOW.plus(TTL));
        ArgumentCaptor<AuthSession> stored = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessions).save(stored.capture());
        assertThat(stored.getValue().getTokenHash()).isEqualTo(AccountService.hash(session.token())).isNotEqualTo(session.token());

        verify(likes).reassign("device1", "acct_7");
        verify(likes).deleteByUser("device1");
        verify(history).reassign("device1", "acct_7");
    }

    @Test
    void signUpRejectsTakenUsername() {
        when(users.existsByUsername("riya")).thenReturn(true);

        assertThatThrownBy(() -> service.signUp("riya", "secret-123", "d"))
                .isInstanceOfSatisfying(AccountException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void signUpValidatesUsernameAndPassword() {
        assertThatThrownBy(() -> service.signUp("ab", "secret-123", "d")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.signUp("riya singh", "secret-123", "d")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.signUp("riya", "short", "d")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.signUp("riya", "x".repeat(73), "d")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void logInChecksPassword() {
        when(users.findByUsername("riya")).thenReturn(Optional.of(account(3, "riya", "secret-123")));

        assertThat(service.logIn("RIYA", "secret-123", "device2").accountId()).isEqualTo(3L);
        verify(history).reassign("device2", "acct_3");

        assertThatThrownBy(() -> service.logIn("riya", "wrong-pass", "device2"))
                .isInstanceOfSatisfying(AccountException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void logInWithUnknownUserIsTheSameError() {
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logIn("nobody", "secret-123", "d"))
                .isInstanceOf(AccountException.class)
                .hasMessage("Wrong username or password");
        verify(sessions, never()).save(any());
    }

    @Test
    void sharedGuestAndReservedIdsAreNeverMerged() {
        when(users.findByUsername("riya")).thenReturn(Optional.of(account(3, "riya", "secret-123")));

        service.logIn("riya", "secret-123", "guest");
        service.logIn("riya", "secret-123", "acct_9");
        service.logIn("riya", "secret-123", null);

        verify(likes, never()).reassign(anyString(), anyString());
        verify(history, never()).reassign(anyString(), anyString());
    }

    @Test
    void sessionsExpireFourHoursAfterLogin() {
        UserAccount riya = account(3, "riya", "secret-123");
        when(sessions.findWithUser(AccountService.hash("live")))
                .thenReturn(Optional.of(new AuthSession("h", riya, NOW.minus(Duration.ofHours(3).plusMinutes(59)))));
        when(sessions.findWithUser(AccountService.hash("stale")))
                .thenReturn(Optional.of(new AuthSession("h", riya, NOW.minus(Duration.ofHours(4)))));

        assertThat(service.authenticate("live")).map(Session::username).contains("riya");
        assertThat(service.authenticate("live")).map(Session::expiresAt).contains(NOW.plusSeconds(60));
        assertThat(service.authenticate("stale")).isEmpty();
        assertThat(service.authenticate("unknown")).isEmpty();
        assertThat(service.authenticate(null)).isEmpty();
    }

    @Test
    void usingASessionDoesNotExtendIt() {
        AuthSession session = new AuthSession("h", account(3, "riya", "secret-123"), NOW.minus(Duration.ofHours(2)));
        when(sessions.findWithUser(AccountService.hash("tok"))).thenReturn(Optional.of(session));

        assertThat(service.authenticate("tok")).map(Session::expiresAt).contains(NOW.plus(Duration.ofHours(2)));
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        assertThat(session.getCreatedAt()).isEqualTo(NOW.minus(Duration.ofHours(2)));
    }

    @Test
    void expiredSessionsArePurged() {
        service.purgeExpiredSessions();

        verify(sessions).deleteCreatedBefore(NOW.minus(TTL));
    }

    @Test
    void resetPasswordSetsNewHashEndsOldSessionsAndLogsIn() {
        UserAccount riya = account(3, "riya", "old-pass-1");
        when(users.findByUsername("riya")).thenReturn(Optional.of(riya));

        Session session = service.resetPassword("Riya", "new-pass-1", "device1");

        assertThat(new BCryptPasswordEncoder().matches("new-pass-1", riya.getPasswordHash())).isTrue();
        verify(sessions).deleteByUserId(3L);
        verify(sessions).save(any());
        assertThat(session.accountId()).isEqualTo(3L);
    }

    @Test
    void resetPasswordValidatesAndNeedsAnExistingAccount() {
        assertThatThrownBy(() -> service.resetPassword("riya", "short", "d")).isInstanceOf(IllegalArgumentException.class);
        when(users.findByUsername("nobody")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("nobody", "new-pass-1", "d"))
                .isInstanceOfSatisfying(AccountException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void resetPasswordCanBeTurnedOff() {
        AccountService locked = new AccountService(users, sessions, likes, history, Clock.fixed(NOW, ZoneOffset.UTC), false, TTL);
        assertThatThrownBy(() -> locked.resetPassword("riya", "new-pass-1", "d"))
                .isInstanceOfSatisfying(AccountException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(users, never()).findByUsername(anyString());
    }

    @Test
    void logOutDeletesTheSession() {
        service.logOut("tok");
        verify(sessions).deleteById(AccountService.hash("tok"));
    }
}
