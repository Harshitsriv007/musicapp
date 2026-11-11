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

    public static AccountException notLoggedIn() {
        return new AccountException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again");
    }
}
