package com.musicapp.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id that likes and history are stored under: the logged-in account
 * ({@code Authorization: Bearer <token>}) or, when not logged in, the device id ({@code X-User-Id}).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}
