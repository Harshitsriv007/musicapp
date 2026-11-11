package com.musicapp.web;

import com.musicapp.account.AccountException;
import com.musicapp.account.AccountService;
import com.musicapp.account.Session;
import com.musicapp.web.dto.AccountDto;
import com.musicapp.web.dto.AuthRequest;
import com.musicapp.web.dto.AuthResponse;
import com.musicapp.web.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Accounts, so likes and history follow a listener to every device. Sign-up and login also
 * move this device's anonymous likes/history ({@code X-User-Id}) into the account.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AccountService accounts;

    public AuthController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(
            @RequestHeader(name = UserIds.HEADER, required = false) String deviceId,
            @Valid @RequestBody AuthRequest request) {
        Session session = accounts.signUp(request.username(), request.password(), deviceId);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(session));
    }

    @PostMapping("/login")
    public AuthResponse logIn(
            @RequestHeader(name = UserIds.HEADER, required = false) String deviceId,
            @Valid @RequestBody AuthRequest request) {
        return AuthResponse.from(accounts.logIn(request.username(), request.password(), deviceId));
    }

    /** "Forgot password": new password for a username; returns a fresh session like login. */
    @PostMapping("/reset-password")
    public AuthResponse resetPassword(
            @RequestHeader(name = UserIds.HEADER, required = false) String deviceId,
            @Valid @RequestBody ResetPasswordRequest request) {
        return AuthResponse.from(accounts.resetPassword(request.username(), request.newPassword(), deviceId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logOut(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        String token = CurrentUserResolver.bearerToken(authorization);
        if (token != null) {
            accounts.logOut(token);
        }
        return ResponseEntity.noContent().build();
    }

    /** Who this token belongs to; 401 when it is missing, unknown or expired. */
    @GetMapping("/me")
    public AccountDto me(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return accounts.authenticate(CurrentUserResolver.bearerToken(authorization))
                .map(s -> new AccountDto(s.username()))
                .orElseThrow(AccountException::notLoggedIn);
    }
}
