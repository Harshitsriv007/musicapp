package com.musicapp.web;

import com.musicapp.account.AccountException;
import com.musicapp.account.AccountService;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    private static final String BEARER = "Bearer ";

    private final AccountService accounts;

    public CurrentUserResolver(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType() == String.class;
    }

    @Override
    public String resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest request, WebDataBinderFactory binderFactory) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            // An expired or unknown token is always 401, so the client sends the listener back to login.
            return accounts.authenticate(bearerToken(authorization))
                    .map(session -> UserIds.forAccount(session.accountId()))
                    .orElseThrow(AccountException::notLoggedIn);
        }
        if (parameter.getParameterAnnotation(CurrentUser.class).required()) {
            throw AccountException.loginRequired();
        }
        return null;
    }

    /** The token from {@code Authorization: Bearer <token>}, or null if the header is not a bearer token. */
    static String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String token = authorization.substring(BEARER.length()).strip();
        return token.isEmpty() ? null : token;
    }
}
