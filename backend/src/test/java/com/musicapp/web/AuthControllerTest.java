package com.musicapp.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.musicapp.account.AccountException;
import com.musicapp.account.AccountService;
import com.musicapp.account.Session;
import com.musicapp.service.LikeService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, LikeController.class})
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AccountService accountService;

    @MockitoBean
    LikeService likeService;

    private static final String BODY = "{\"username\":\"riya\",\"password\":\"secret-123\"}";

    @Test
    void signUpReturnsTokenAndPassesDeviceId() throws Exception {
        when(accountService.signUp("riya", "secret-123", "device1")).thenReturn(new Session("tok", 7, "riya"));

        mvc.perform(post("/api/auth/signup").header("X-User-Id", "device1")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("tok"))
                .andExpect(jsonPath("$.username").value("riya"));
    }

    @Test
    void signUpWithTakenNameIsConflict() throws Exception {
        when(accountService.signUp(anyString(), anyString(), any())).thenThrow(AccountException.usernameTaken());

        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("That username is already taken"));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        when(accountService.logIn(anyString(), anyString(), any())).thenThrow(AccountException.badCredentials());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginValidatesBody() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"riya\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accountService);
    }

    @Test
    void resetPasswordReturnsANewSession() throws Exception {
        when(accountService.resetPassword("riya", "new-pass-1", "device1")).thenReturn(new Session("tok2", 7, "riya"));

        mvc.perform(post("/api/auth/reset-password").header("X-User-Id", "device1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"riya\",\"newPassword\":\"new-pass-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok2"));
    }

    @Test
    void meAndLogout() throws Exception {
        when(accountService.authenticate("tok")).thenReturn(Optional.of(new Session("tok", 7, "riya")));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("riya"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer tok")).andExpect(status().isNoContent());
        verify(accountService).logOut("tok");
    }

    @Test
    void loggedInRequestsUseTheAccountInsteadOfTheDevice() throws Exception {
        when(accountService.authenticate("tok")).thenReturn(Optional.of(new Session("tok", 7, "riya")));

        mvc.perform(put("/api/likes/5").header("Authorization", "Bearer tok").header("X-User-Id", "device1"))
                .andExpect(status().isNoContent());
        verify(likeService).like("acct_7", 5L);
    }

    @Test
    void expiredTokenIsUnauthorizedNotSilentlyAnonymous() throws Exception {
        mvc.perform(put("/api/likes/5").header("Authorization", "Bearer gone").header("X-User-Id", "device1"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(likeService);
    }

    @Test
    void devicesCannotImpersonateAnAccount() throws Exception {
        mvc.perform(put("/api/likes/5").header("X-User-Id", "acct_7")).andExpect(status().isBadRequest());
        verifyNoInteractions(likeService);
    }
}
