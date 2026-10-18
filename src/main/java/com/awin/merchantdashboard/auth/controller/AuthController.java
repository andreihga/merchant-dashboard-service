package com.awin.merchantdashboard.auth.controller;

import com.awin.merchantdashboard.api.AuthApi;
import com.awin.merchantdashboard.api.model.TokenRequest;
import com.awin.merchantdashboard.api.model.TokenResponse;
import com.awin.merchantdashboard.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuthController implements AuthApi {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<TokenResponse> issueToken(TokenRequest tokenRequest) {
        return ResponseEntity.ok(authService.issueToken(tokenRequest.getUsername(), tokenRequest.getPassword()));
    }
}
