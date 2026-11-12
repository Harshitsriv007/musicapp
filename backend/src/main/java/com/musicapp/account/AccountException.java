package com.musicapp.account;

import org.springframework.http.HttpStatus;

/** Sign-up / login failures, mapped to an HTTP status by the exception handler. */
public class AccountException extends RuntimeException {

    private final HttpStatus status;

    private AccountException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }

    public static AccountException usernameTaken() {
        return new AccountException(HttpStatus.CONFLICT, "That username is already taken");
    }

    public static AccountException badCredentials() {
        return new AccountException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
    }

    public static AccountException unknownUser() {
        return new AccountException(HttpStatus.NOT_FOUND, "No account with that username");
    }

    public static AccountException resetDisabled() {
        return new AccountException(HttpStatus.FORBIDDEN, "Password reset is turned off on this server");
    }

    public static AccountException loginRequired() {
        return new AccountException(HttpStatus.UNAUTHORIZED, "Please log in");
    }

    public static AccountException notLoggedIn() {
        return new AccountException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again");
    }
}
