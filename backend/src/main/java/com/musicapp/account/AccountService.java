package com.musicapp.account;

import com.musicapp.domain.AuthSession;
import com.musicapp.domain.UserAccount;
import com.musicapp.repository.AuthSessionRepository;
import com.musicapp.repository.LikedSongRepository;
import com.musicapp.repository.PlayHistoryRepository;
import com.musicapp.repository.UserAccountRepository;
import com.musicapp.web.UserIds;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    /** A session stays valid while it is used at least once in this period. */
    static final Duration SESSION_IDLE_TIMEOUT = Duration.ofDays(180);
    /** last_used_at is written at most this often, not on every request. */
    private static final Duration TOUCH_INTERVAL = Duration.ofHours(12);

    private static final Pattern USERNAME = Pattern.compile("[a-z0-9_.-]{3,32}");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD_BYTES = 72; // BCrypt limit

    private final UserAccountRepository users;
    private final AuthSessionRepository sessions;
    private final LikedSongRepository likes;
    private final PlayHistoryRepository history;
    private final Clock clock;
    private final boolean passwordResetEnabled;
    private final PasswordEncoder passwords = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    /** Compared against when the username is unknown, so both cases take the same time. */
    private final String dummyHash = passwords.encode("not-a-real-password");

    public AccountService(UserAccountRepository users, AuthSessionRepository sessions,
                          LikedSongRepository likes, PlayHistoryRepository history, Clock clock,
                          @Value("${musicapp.auth.password-reset-enabled:true}") boolean passwordResetEnabled) {
        this.users = users;
        this.sessions = sessions;
        this.likes = likes;
        this.history = history;
        this.clock = clock;
        this.passwordResetEnabled = passwordResetEnabled;
    }

    /**
     * Creates an account and logs this device in. Likes and history the device collected
     * while anonymous ({@code deviceId}) move into the account.
     */
    @Transactional
    public Session signUp(String rawUsername, String password, String deviceId) {
        String username = normalizeUsername(rawUsername);
        if (!USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("Username must be 3-32 characters: letters, digits, '.', '_' or '-'");
        }
        validatePassword(password);
        if (users.existsByUsername(username)) {
            throw AccountException.usernameTaken();
        }
        UserAccount account;
        try {
            account = users.saveAndFlush(new UserAccount(username, passwords.encode(password), Instant.now(clock)));
        } catch (DataIntegrityViolationException raced) {
            throw AccountException.usernameTaken();
        }
        return startSession(account, deviceId);
    }

    @Transactional
    public Session logIn(String rawUsername, String password, String deviceId) {
        Optional<UserAccount> account = users.findByUsername(normalizeUsername(rawUsername));
        String hash = account.map(UserAccount::getPasswordHash).orElse(dummyHash);
        boolean matches = password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && passwords.matches(password, hash);
        if (account.isEmpty() || !matches) {
            throw AccountException.badCredentials();
        }
        return startSession(account.get(), deviceId);
    }

    /**
     * "Forgot password": sets a new password from just the username, then logs this device in.
     * There is deliberately no verification (no email / recovery code), so anyone who can reach
     * the server can reset any account. Every existing session is ended. Turn it off with
     * PASSWORD_RESET_ENABLED=false before exposing the app to people you don't trust.
     */
    @Transactional
    public Session resetPassword(String rawUsername, String newPassword, String deviceId) {
        if (!passwordResetEnabled) {
            throw AccountException.resetDisabled();
        }
        validatePassword(newPassword);
        UserAccount account = users.findByUsername(normalizeUsername(rawUsername))
                .orElseThrow(AccountException::unknownUser);
        account.setPasswordHash(passwords.encode(newPassword));
        sessions.deleteByUserId(account.getId());
        return startSession(account, deviceId);
    }

    @Transactional
    public void logOut(String token) {
        sessions.deleteById(hash(token));
    }

    /** The account a bearer token belongs to, or empty if the token is unknown or expired. */
    @Transactional
    public Optional<Session> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        return sessions.findWithUser(hash(token))
                .filter(s -> s.getLastUsedAt().plus(SESSION_IDLE_TIMEOUT).isAfter(now))
                .map(s -> {
                    if (s.getLastUsedAt().plus(TOUCH_INTERVAL).isBefore(now)) {
                        s.touch(now);
                    }
                    UserAccount user = s.getUser();
                    return new Session(token, user.getId(), user.getUsername());
                });
    }

    private Session startSession(UserAccount account, String deviceId) {
        mergeDevice(deviceId, UserIds.forAccount(account.getId()));
        String token = newToken();
        sessions.save(new AuthSession(hash(token), account, Instant.now(clock)));
        return new Session(token, account.getId(), account.getUsername());
    }

    /** Moves an anonymous device's likes and history into the account. The shared "guest" id is never merged. */
    private void mergeDevice(String deviceId, String accountKey) {
        if (deviceId == null || UserIds.DEFAULT.equals(deviceId)) {
            return;
        }
        try {
            UserIds.require(deviceId);
        } catch (IllegalArgumentException invalid) {
            return;
        }
        likes.reassign(deviceId, accountKey);
        likes.deleteByUser(deviceId); // leftovers were songs the account had already liked
        history.reassign(deviceId, accountKey);
    }

    private static String normalizeUsername(String raw) {
        return raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password is too long");
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
