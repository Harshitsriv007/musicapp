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
            // A client that sends a token expects its account; never silently fall back to the device.
            return accounts.authenticate(bearerToken(authorization))
                    .map(session -> UserIds.forAccount(session.accountId()))
                    .orElseThrow(AccountException::notLoggedIn);
        }
        String device = request.getHeader(UserIds.HEADER);
        return UserIds.require(device == null ? UserIds.DEFAULT : device);
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
