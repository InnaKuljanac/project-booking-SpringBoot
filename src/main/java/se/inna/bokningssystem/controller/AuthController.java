package se.inna.bokningssystem.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inna.bokningssystem.dto.AuthResponseDto;
import se.inna.bokningssystem.dto.LoginRequestDto;
import se.inna.bokningssystem.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto dto) {

        return ResponseEntity.ok(authService.login(dto));
    }
}