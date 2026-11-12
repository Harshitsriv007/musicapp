package com.musicapp.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id that likes, history and downloads are stored under: the logged-in account
 * ({@code Authorization: Bearer <token>}). Listener data is never readable without a valid session,
 * so one account can never see another's.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {

    /**
     * False for public catalogue endpoints: a visitor without a token gets {@code null} (nothing
     * personal is shown). A token that is sent must still be valid, otherwise the request is 401.
     */
    boolean required() default true;
}
